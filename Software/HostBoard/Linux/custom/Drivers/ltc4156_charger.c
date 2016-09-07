/*
 * Copyright 2015 EUROPEAN UNION
 * 
 * Date: 02/04/2015
 * Authors
 * - Marco Signorini, marco.signorini@liberaintentio.com
 *
 * - Michel Gerboles, michel.gerboles@jrc.ec.europa.eu, 
 *   Laurent Spinelle, laurent.spinelle@jrc.ec.europa.eu and 
 *   Alexander Kotsev, alexander.kotsev@jrc.ec.europa.eu:
 *			European Commission - Joint Research Centre, 
 * 
 *  Driver for chargers build on LTC4156 devices.
 *  Based on Auryn Verwegen and Mike Looijmans work
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under  the terms of the GNU General  Public License as published by the
 *  Free Software Foundation;  either version 2 of the License, or (at your
 *  option) any later version.

 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

#include <linux/kernel.h>
#include <linux/module.h>
#include <linux/types.h>
#include <linux/errno.h>
#include <linux/swab.h>
#include <linux/i2c.h>
#include <linux/delay.h>
#include <linux/idr.h>
#include <linux/power_supply.h>
#include <linux/slab.h>

#define I16_MSB(x)                      ((x >> 8) & 0xFF)
#define I16_LSB(x)                      (x & 0xFF)
#define SET_REGISTER_PART(r,x,mask)     (((r) & (0xFF^(mask))) | ((x) & (mask)))  

#define LTC4156_WORK_DELAY        10     /* Update delay in seconds */
#define LTC4156_NUM_REGS          7      /* Number of registers in the device */
#define LTC4156_MAXSETRETRY       10     /* Max number the driver retries to update limits registers */

#define CURLIMIT_MASK             0x1F
#define SAFETYTIMER_MASK          0x60
#define BATTERYVOLTAGE_MASK       0x0C

#define CHARGER_STATUS(x)               (((x) & 0xE0) >> 5)
#define CURRENTLIMIT_REGVAL(x)          ((x) & CURLIMIT_MASK)
#define SET_CURRENTLIMIT_REGVAL(r,x)    SET_REGISTER_PART(r,x,CURLIMIT_MASK)

#define SAFETYTIMER_REGVAL(x)           ((x) & SAFETYTIMER_MASK)
#define SET_SAFETYTIMER_REGVAL(r,x)     SET_REGISTER_PART(r,x,SAFETYTIMER_MASK)

#define BATTERYVOLTAGE_REGVAL(x)        ((x) & BATTERYVOLTAGE_MASK)
#define SET_BATTERYVOLTAGE_REGVAL(r,x)  SET_REGISTER_PART(r,x,BATTERYVOLTAGE_MASK)

#define USBSNS_GOOD(x)                (((x) & 0x40) != 0)
#define WALLSNS_GOOD(x)               (((x) & 0x20) != 0)
#define AT_INPUT_ILIM(x)              (((x) & 0x10) != 0)
#define INPUT_UVCL_ACTIVE(x)          (((x) & 0x08) != 0)
#define OVP_ACTIVE(x)                 (((x) & 0x04) != 0)
#define BAD_CELL(x)                   (((x) & 0x01) != 0)


/*
 * All voltages, currents, charges, energies, time and temperatures in uV,
 * µA, µAh, µWh, seconds and tenths of degree Celsius unless otherwise
 * stated. It's driver's job to convert its raw values to units in which
 * this class operates.
 */


struct ltc4156_info {

  struct i2c_client *client; 
  struct power_supply *supply;
  struct power_supply_desc supply_desc;
  struct delayed_work work;
  u8 num_regs;
  int id;
  bool wakeup_enabled;
  u8 r_usb_curlimit;			/* Required USB current limit */
  u8 r_wall_curlimit;			/* Required Wall current limit */
  u8 r_safety_timer;			/* Required safety timer */
  u8 r_battery_voltage;         /* Required battery voltage */

  bool c_usbsns_active;         /* USB Sense active */
  bool c_wallsns_active;        /* Wall Sense active */
  bool c_ovp_active;			/* Overvoltage protection active */
  bool c_ucvl_active;			/* Undervoltage protection atvice */
  bool c_at_input_ilim;         /* Input current protection active */
  bool c_bad_cell;				/* Bad cell detected */

  bool c_charging;				/* Charging now */

  u8	num_retry_toset;		/* Number of times the driver tried to set current limit */
  u8	c_usb_curlimit;			/* Current USB current limit */
  u8	c_wall_curlimit;		/* Current Wall current limit */
  u8	c_safety_timer;			/* Current safety timer */
  u8	c_battery_voltage;      /* Current battery voltage set */
};

enum ltc4156_charger_reg {
  LTC4156_REG0 = 0x00,
  LTC4156_REG1 = 0x01,
  LTC4156_REG2 = 0x02,
  LTC4156_REG3 = 0x03,
  LTC4156_REG4 = 0x04,
  LTC4156_REG5 = 0x05,
  LTC4156_REG6 = 0x06,
};

struct ltc4156_charger_userpar {
  u32 user_val;
  u8 reg_val;
};

static struct ltc4156_charger_userpar ltc4156_charger_curlimits[] = {
  { -1, 31 }, { 2500, 15 }, { 100000, 0 }, { 500000, 1 }, { 600000, 2 }, { 700000, 3 }, { 800000, 4 }, 	
  { 900000, 5 }, { 1000000, 6 }, { 1250000, 7 }, { 1500000, 8 }, { 1750000, 9 }, { 2000000, 10 },
  { 2250000, 11 }, { 2500000, 12 }, { 2750000, 13 }, { 3000000, 14 },
};


static struct ltc4156_charger_userpar ltc4156_charger_battery_curlimits[] = {
  { 0, 0x00 }, { 1250, 0x10 }, { 1875, 0x20 }, { 2500, 0x30 }, { 3125, 0x40 }, { 3750, 0x50 }, 
  { 4375, 0x60 }, { 5000, 0x70 }, { 5625, 0x80 }, { 6250, 0x90 }, { 6875, 0xA0 }, 
  { 7500, 0xB0 }, { 8125, 0xC0 }, { 8750, 0xD0 }, { 9375, 0xE0 }, { 10000, 0xF0 }, 
};

static struct ltc4156_charger_userpar ltc4156_charger_safetytimers[] = {
  { 900, 0x60 }, { 1800, 0x40 }, { 3600, 0x00 }, { 14400, 0x20 }, 
};

static struct ltc4156_charger_userpar ltc4156_charger_battery_voltages[] = {
  { 3450000, 0x00 }, { 3550000, 0x04 }, { 3600000, 0x08 }, { 3800000, 0x0C },
};

static DEFINE_IDR(ltc4156_id);
static DEFINE_MUTEX(ltc4156_lock);


static int ltc4156_charger_from_userpar_to_register(struct ltc4156_charger_userpar* list, int size, u32 userval) {
  int n;

  for (n = 1; n < size; n++) {
      if (list[n].user_val == userval) {
        return list[n].reg_val;
      } 
      if (list[n].user_val > userval) {
        return list[n-1].reg_val;
      }
  }

  return -1;
}

static int ltc4156_charger_from_register_to_userpar(struct ltc4156_charger_userpar* list, int size, u8 regval) {
  int n;

  for (n = 0; n < size; n++) {
    if (list[n].reg_val == regval) {
      return list[n].user_val;
    }
  }

  return -EINVAL;
}

static int ltc4156_charger_from_curlimit_to_register(u32 curlimit) {
  int result;

  if (curlimit == -1) {
      return ltc4156_charger_curlimits[0].reg_val;
  }

  result = ltc4156_charger_from_userpar_to_register(ltc4156_charger_curlimits, 
                                          ARRAY_SIZE(ltc4156_charger_curlimits), curlimit);

  if (result == -1)	{
    return ltc4156_charger_curlimits[ARRAY_SIZE(ltc4156_charger_curlimits)-1].reg_val;		
  } 

  return result;
}

static int ltc4156_charger_from_register_to_curlimit(u8 regval) {

  regval &= CURLIMIT_MASK;
  return ltc4156_charger_from_register_to_userpar(ltc4156_charger_curlimits,
          ARRAY_SIZE(ltc4156_charger_curlimits), regval);
}

static int ltc4156_charger_from_safetytimer_to_register(u32 safetytimer) {
  int result;

  result = ltc4156_charger_from_userpar_to_register(ltc4156_charger_safetytimers,
          ARRAY_SIZE(ltc4156_charger_safetytimers), safetytimer);

  if (result == -1) {
    return 0x00;
  }

  return result;
}

static int ltc4156_charger_from_batteryvoltage_to_register(u32 voltage) {
  int result;

  result = ltc4156_charger_from_userpar_to_register(ltc4156_charger_battery_voltages,
          ARRAY_SIZE(ltc4156_charger_battery_voltages), voltage);

  if (result == -1) {
    return ltc4156_charger_battery_voltages[0].reg_val;
  }

  return result;
}

static int ltc4156_charger_from_register_to_batteryvoltage(u8 regval) {

  regval &= BATTERYVOLTAGE_MASK;
  return ltc4156_charger_from_register_to_userpar(ltc4156_charger_battery_voltages,
          ARRAY_SIZE(ltc4156_charger_battery_voltages), regval);
}

static int _ltc4156_charger_read_regs(struct i2c_client *client,
        enum ltc4156_charger_reg reg, u8 *buf, int num_regs) {
  int ret;
  struct i2c_msg msgs[2] = {};
  u8 reg_start = reg;

  msgs[0].addr = client->addr;
  msgs[0].len = 1;
  msgs[0].buf = &reg_start;

  msgs[1].addr = client->addr;
  msgs[1].len = num_regs;
  msgs[1].buf = buf;
  msgs[1].flags = I2C_M_RD;

  ret = i2c_transfer(client->adapter, &msgs[0], 2);
  if (ret < 0) {
    dev_err(&client->dev, "ltc4156 read_reg failed!\n");
    return ret;
  }

  dev_dbg(&client->dev, "%s (%#x, %d) -> %#x\n",
          __func__, reg, num_regs, *buf);

  return 0;
}

static int ltc4156_charger_read_regs(struct i2c_client *client,
        enum ltc4156_charger_reg reg, u8 *buf, int num_regs) {
  
  int n;
  int ret;
  
  for (n = 0; n < num_regs; n++) {
    ret = _ltc4156_charger_read_regs(client, reg+n, buf++, 1);
    if (ret < 0) {
      return ret;
    }
  }
  
  return 0;
}

static int ltc4156_charger_write_regs(struct i2c_client *client,
        enum ltc4156_charger_reg reg, const u8 *buf, int num_regs) {
  int ret;
  u8 reg_start = reg;

  ret = i2c_smbus_write_i2c_block_data(client, reg_start, num_regs, buf);
  if (ret < 0) {
    dev_err(&client->dev, "ltc4156 write_reg failed!\n");
    return ret;
  }

  dev_dbg(&client->dev, "%s (%#x, %d) -> %#x\n",
          __func__, reg, num_regs, *buf);

  return 0;
}

static int ltc4156_charger_reset(const struct ltc4156_info *info) {

  int ret;
  u8 value;

  /* Read a register to check the chip presence */
  ret = ltc4156_charger_read_regs(info->client, LTC4156_REG0, &value, 1);
  if (ret < 0) {
    dev_err(&info->client->dev,
            "Could not read registers from device\n");
    goto error_exit;
  }

  return 0;

error_exit:
  return ret;
}

static void ltc4156_charger_update(struct ltc4156_info *info) {
  int ret;
  bool changed;
  bool power_changed;
  u8 regs[LTC4156_NUM_REGS];
  u8 value;

  ret = ltc4156_charger_read_regs(info->client, LTC4156_REG0, regs, LTC4156_NUM_REGS);
  if (ret < 0) {
    goto error_exit;
  }

  info->c_usb_curlimit = CURRENTLIMIT_REGVAL(regs[LTC4156_REG0]);

  info->c_wall_curlimit = CURRENTLIMIT_REGVAL(regs[LTC4156_REG1]);
  info->c_safety_timer = SAFETYTIMER_REGVAL(regs[LTC4156_REG1]);

  info->c_battery_voltage = BATTERYVOLTAGE_REGVAL(regs[LTC4156_REG2]);

  changed = (info->c_charging != (CHARGER_STATUS(regs[LTC4156_REG3]) != 0));
  info->c_charging = (CHARGER_STATUS(regs[LTC4156_REG3]) != 0);

  power_changed = (info->c_usbsns_active != USBSNS_GOOD(regs[LTC4156_REG4]));
  info->c_usbsns_active = USBSNS_GOOD(regs[LTC4156_REG4]);
  power_changed |= (info->c_wallsns_active != WALLSNS_GOOD(regs[LTC4156_REG4]));
  info->c_wallsns_active = WALLSNS_GOOD(regs[LTC4156_REG4]);
  info->c_ovp_active = OVP_ACTIVE(regs[LTC4156_REG4]);
  info->c_ucvl_active = INPUT_UVCL_ACTIVE(regs[LTC4156_REG4]);
  info->c_at_input_ilim = AT_INPUT_ILIM(regs[LTC4156_REG4]);
  info->c_bad_cell = BAD_CELL(regs[LTC4156_REG4]);

  changed |= (info->r_safety_timer != info->c_safety_timer);

  if (power_changed) {
    info->num_retry_toset = 0;
  }

  /* Update registers if needed */
  if ((power_changed && info->c_usbsns_active) ||
          (!power_changed && (info->num_retry_toset < LTC4156_MAXSETRETRY) && info->c_usbsns_active && (info->r_usb_curlimit != info->c_usb_curlimit))) {

    info->num_retry_toset++;
    value = SET_CURRENTLIMIT_REGVAL(regs[LTC4156_REG0], info->r_usb_curlimit);
    ret = ltc4156_charger_write_regs(info->client, LTC4156_REG0, &value, 1);
    if (ret < 0) {
      goto error_exit;
    }
  }
  
  if ((power_changed && info->c_wallsns_active) ||
          (!power_changed && (info->num_retry_toset < LTC4156_MAXSETRETRY) && info->c_wallsns_active && (info->r_wall_curlimit != info->c_wall_curlimit)) ||
          (info->r_safety_timer != info->c_safety_timer)) {

    value = SET_SAFETYTIMER_REGVAL(regs[LTC4156_REG1], info->r_safety_timer);

    if ((power_changed && info->c_wallsns_active) ||
            (!power_changed && (info->num_retry_toset < LTC4156_MAXSETRETRY) && info->c_wallsns_active && (info->r_wall_curlimit != info->c_wall_curlimit))) {

      info->num_retry_toset++;
      value = SET_CURRENTLIMIT_REGVAL(value, info->r_wall_curlimit);
    }

    ret = ltc4156_charger_write_regs(info->client, LTC4156_REG1, &value, 1);
    if (ret < 0) {
      goto error_exit;
    }
  }

  if (power_changed || (info->r_battery_voltage != info->c_battery_voltage)) {

    value = SET_BATTERYVOLTAGE_REGVAL(regs[LTC4156_REG2], info->r_battery_voltage);
    ret = ltc4156_charger_write_regs(info->client, LTC4156_REG2, &value, 1);
    if (ret < 0) {
      goto error_exit;
    }
  }

  /* Kick back an udpate if needed */
  if (changed || power_changed) {
    power_supply_changed(info->supply);
  }
  
  return;

error_exit:
  dev_err(&info->client->dev, "Could not read or write registers to device\n");
  return;
}

static void ltc4156_charger_work(struct work_struct *work) {
  struct ltc4156_info *info;

  info = container_of(work, struct ltc4156_info, work.work);
  ltc4156_charger_update(info);
  schedule_delayed_work(&info->work, LTC4156_WORK_DELAY * HZ);
}

static int ltc4156_charger_get_property(struct power_supply *psy,
        enum power_supply_property psp, union power_supply_propval *val) {

  struct ltc4156_info *ltc4156_info = power_supply_get_drvdata(psy);

  switch (psp) {
    case POWER_SUPPLY_PROP_STATUS:
      if (ltc4156_info->c_usbsns_active || ltc4156_info->c_wallsns_active) {
        if (ltc4156_info->c_charging) {
          val->intval = POWER_SUPPLY_STATUS_CHARGING;
        } else {
          val->intval = POWER_SUPPLY_STATUS_FULL;
        }
      } else {
        val->intval = POWER_SUPPLY_STATUS_DISCHARGING;
      }
      break;
    case POWER_SUPPLY_PROP_HEALTH:
      val->intval = (ltc4156_info->c_bad_cell)? POWER_SUPPLY_HEALTH_DEAD : POWER_SUPPLY_HEALTH_GOOD;
      break;
    case POWER_SUPPLY_PROP_ONLINE:
      val -> intval = (ltc4156_info->c_usbsns_active || ltc4156_info->c_wallsns_active) ? 1 : 0;
      break;
    case POWER_SUPPLY_PROP_TECHNOLOGY:
      val->intval = (int) POWER_SUPPLY_TECHNOLOGY_LiFe;
      break;
    case POWER_SUPPLY_PROP_INPUT_CURRENT_LIMIT:
      val->intval = ltc4156_charger_from_register_to_curlimit((ltc4156_info->c_usbsns_active)? ltc4156_info->c_usb_curlimit : ltc4156_info->c_wall_curlimit);
      break;
    case POWER_SUPPLY_PROP_CONSTANT_CHARGE_CURRENT_MAX:
      val->intval = 0;
      break;
    case POWER_SUPPLY_PROP_CONSTANT_CHARGE_VOLTAGE_MAX:
      val->intval = ltc4156_charger_from_register_to_batteryvoltage(ltc4156_info->r_battery_voltage);
      break;

    default:
      return -EINVAL;
  }

  return 0;
}

static int ltc4156_charger_set_property(struct power_supply *psy,
        enum power_supply_property psp,
        const union power_supply_propval *val) {

  struct ltc4156_info *info = power_supply_get_drvdata(psy);

  switch (psp) {
    case POWER_SUPPLY_PROP_INPUT_CURRENT_LIMIT:
      info->r_wall_curlimit = ltc4156_charger_from_curlimit_to_register(val->intval);
      info->num_retry_toset = 0;
      break;

    case POWER_SUPPLY_PROP_CONSTANT_CHARGE_CURRENT_MAX:
      break;

    case POWER_SUPPLY_PROP_CONSTANT_CHARGE_VOLTAGE_MAX:
      info->r_battery_voltage = ltc4156_charger_from_batteryvoltage_to_register(val->intval);
      break;

    default:
      return -EPERM;
  }

  return 0;
}

static int ltc4156_charger_property_is_writeable(
        struct power_supply *psy, enum power_supply_property psp) {
  switch (psp) {
    case POWER_SUPPLY_PROP_INPUT_CURRENT_LIMIT:
    case POWER_SUPPLY_PROP_CONSTANT_CHARGE_CURRENT_MAX:
    case POWER_SUPPLY_PROP_CONSTANT_CHARGE_VOLTAGE_MAX:
      return 1;
    default:
      return 0;
  }
}

static enum power_supply_property ltc4156_charger_properties[] = {
  POWER_SUPPLY_PROP_STATUS,
  POWER_SUPPLY_PROP_HEALTH, /* POWER_SUPPLY_HEALTH_GOOD, POWER_SUPPLY_HEALTH_COLD, POWER_SUPPLY_HEALTH_OVERHEAT, POWER_SUPPLY_HEALTH_DEAD */
  POWER_SUPPLY_PROP_TECHNOLOGY,
  POWER_SUPPLY_PROP_ONLINE,
  POWER_SUPPLY_PROP_INPUT_CURRENT_LIMIT,
  POWER_SUPPLY_PROP_CONSTANT_CHARGE_CURRENT_MAX,
  POWER_SUPPLY_PROP_CONSTANT_CHARGE_VOLTAGE_MAX,
};

static int ltc4156_charger_parse_dt(struct device *dev, struct ltc4156_info *info) {
  struct device_node *np = dev->of_node;
  int ret;
  u32 u32value;

  if (!np || !info)
    return -ENOENT;

  ret = of_property_read_u32(np, "usb,currentlimit", &u32value);
  if (ret >= 0) {
    dev_err(dev, "USB current limit required: %d", u32value);
    info->r_usb_curlimit = ltc4156_charger_from_curlimit_to_register(u32value);
  }

  ret = of_property_read_u32(np, "wal,currentlimit", &u32value);
  if (ret >= 0) {
    dev_err(dev, "WALL current limit required: %d", u32value);
    info->r_wall_curlimit = ltc4156_charger_from_curlimit_to_register(u32value);
  }

  ret = of_property_read_u32(np, "battery-voltage", &u32value);
  if (ret >= 0) {
    dev_err(dev, "Battery voltage required %d", u32value);
    info->r_battery_voltage = ltc4156_charger_from_batteryvoltage_to_register(u32value);
  }

  ret = of_property_read_u32(np, "safety-timer", &u32value);
  if (ret >= 0) {
    dev_err(dev, "Safety timer required %d", u32value);
    info->r_safety_timer = ltc4156_charger_from_safetytimer_to_register(u32value);
  }

  return 0;
}

static int ltc4156_charger_probe(struct i2c_client *client,
        const struct i2c_device_id *id) {

  struct power_supply_config psy_cfg = {};
  struct ltc4156_info *info;
  int ret;
  int num;

  mutex_lock(&ltc4156_lock);
  ret = idr_alloc(&ltc4156_id, client, 0, 0, GFP_KERNEL);
  mutex_unlock(&ltc4156_lock);
  if (ret < 0)
    goto fail_id;

  num = ret;

  info = devm_kzalloc(&client->dev, sizeof (*info), GFP_KERNEL);
  if (info == NULL) {
    ret = -ENOMEM;
    goto fail_info;
  }

  i2c_set_clientdata(client, info);

  info->num_regs = id->driver_data;
  info->supply_desc.name = kasprintf(GFP_KERNEL, "%s-%d", client->name, num);
  if (!info->supply_desc.name) {
    ret = -ENOMEM;
    goto fail_name;
  }

  ret = ltc4156_charger_parse_dt(&client->dev, info);
  if (ret < 0) {
    goto fail_parsetree;
  }

  info->client = client;
  info->id = num;
  info->supply_desc.type = POWER_SUPPLY_TYPE_BATTERY;
  info->supply_desc.properties = ltc4156_charger_properties;
  info->supply_desc.num_properties = ARRAY_SIZE(ltc4156_charger_properties);
  info->supply_desc.get_property = ltc4156_charger_get_property;
  info->supply_desc.set_property = ltc4156_charger_set_property;
  info->supply_desc.property_is_writeable = ltc4156_charger_property_is_writeable;
  info->supply_desc.external_power_changed = NULL;
  info->num_retry_toset = 0;

  INIT_DELAYED_WORK(&info->work, ltc4156_charger_work);

  ret = ltc4156_charger_reset(info);
  if (ret < 0) {
    dev_err(&client->dev, "Communication with chip failed\n");
    goto fail_comm;
  }

  psy_cfg.drv_data = info;

  info->supply = power_supply_register(&client->dev, &info->supply_desc, &psy_cfg);

  if (IS_ERR(info->supply)) {
    dev_err(&client->dev, "failed to register ltc4156\n");
    goto fail_register;
  } else {
    schedule_delayed_work(&info->work, LTC4156_WORK_DELAY * HZ);
  }

  return 0;

fail_register:
  kfree(info->supply_desc.name);
fail_comm:
fail_name :
fail_info :
fail_parsetree :
  mutex_lock(&ltc4156_lock);
  idr_remove(&ltc4156_id, num);
  mutex_unlock(&ltc4156_lock);
fail_id:
  return ret;
}

static int ltc4156_charger_remove(struct i2c_client *client) {
  struct ltc4156_info *info = i2c_get_clientdata(client);

  cancel_delayed_work(&info->work);
  power_supply_unregister(info->supply);
  kfree(info->supply_desc.name);
  mutex_lock(&ltc4156_lock);
  idr_remove(&ltc4156_id, info->id);
  mutex_unlock(&ltc4156_lock);
  return 0;
}

#ifdef CONFIG_PM_SLEEP

static int ltc4156_charger_suspend(struct device *dev) {
  struct i2c_client *client = to_i2c_client(dev);
  struct ltc4156_info *info = i2c_get_clientdata(client);

  cancel_delayed_work(&info->work);
  return 0;
}

static int ltc4156_charger_resume(struct device *dev) {
  struct i2c_client *client = to_i2c_client(dev);
  struct ltc4156_info *info = i2c_get_clientdata(client);

  schedule_delayed_work(&info->work, LTC4156_WORK_DELAY * HZ);
  return 0;
}
#endif

static SIMPLE_DEV_PM_OPS(ltc4156_charger_pm_ops,
        ltc4156_charger_suspend, ltc4156_charger_resume);

static const struct i2c_device_id ltc4156_charger_i2c_id[] = {
  { "ltc4156-charger", LTC4156_NUM_REGS},
  {}
};
MODULE_DEVICE_TABLE(i2c, ltc4156_charger_i2c_id);

static struct i2c_driver ltc4156_charger_driver = {
  .driver =
  {
    .name = "ltc4156-charger",
    .pm = &ltc4156_charger_pm_ops,
  },
  .probe = ltc4156_charger_probe,
  .remove = ltc4156_charger_remove,
  .id_table = ltc4156_charger_i2c_id,
};
module_i2c_driver(ltc4156_charger_driver);

MODULE_AUTHOR("Marco Signorini <marco.signorini@liberaintentio.com>");
MODULE_DESCRIPTION("Driver for LiFePO4 battery chargers based on Linear Technology LTC4156 chipset");
MODULE_LICENSE("GPL");
MODULE_ALIAS("i2c:ltc4156-charger");
