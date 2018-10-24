/* ===========================================================================
 * Copyright 2015 EUROPEAN UNION
 *
 * Licensed under the EUPL, Version 1.1 or subsequent versions of the
 * EUPL (the "License"); You may not use this work except in compliance
 * with the License. You may obtain a copy of the License at
 * http://ec.europa.eu/idabc/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Date: 02/04/2015
 * Authors:
 * - Michel Gerboles, michel.gerboles@jrc.ec.europa.eu, 
 *   Laurent Spinelle, laurent.spinelle@jrc.ec.europa.eu and 
 *   Alexander Kotsev, alexander.kotsev@jrc.ec.europa.eu:
 *			European Commission - Joint Research Centre, 
 * - Marco Signorini, marco.signorini@liberaintentio.com
 *
 * ===========================================================================
 */

#if defined(__XC)
    #include <xc.h>         /* XC8 General Include File */
#elif defined(HI_TECH_C)
    #include <htc.h>        /* HiTech General Include File */
#endif

#include <stdint.h>         /* For uint8_t definition */
#include <stdbool.h>        /* For true/false definition */

#include "user.h"
#include "mcc_generated_files/interrupt_manager.h"
#include "mcc_generated_files/tmr2.h"
#include "mcc_generated_files/adc1.h"

// #define HWREV_20

// RA0: Output -> Shutdown Request
// RA1: Unused in HWREV < 20; VBat in HWREV > 20
// RA2: Output -> Power supply
// RA3: Input <- In Power On Pushbutton
// RA4: Input <- 3v3 available 
// RA5: Output -> WakeUp pulse

#define FW_REVISION             "R1.4"

#define POWERONPRESSED          !RA3

#define INCCLAMPTOBYTE(x)       if ((x) < 0xFF) { x++; }
#define INCCLAMPTOINT(x)        if ((x) < 0xFFFF) { x++; }

#define POWERONTHRESHOLD        10      /* Time needed to push the power button to power up the system (0.1s) */
#define SHUTDOWNTHRESHOLD       50      /* Time needed to push the power button to shutdown the system (0.5s) */
#define PRERUNNINGTHRESHOLD     240     /* "Grace" period running before polling for shutdown events */
#define V3VOLTAGETRESHOLD       950     /* Voltage threshold for detection of 3v3. 
                                            (950/1024)*VRef = 1.9V */

#define V3VOLTAGELOSSTIMER      1       /* When running, check multiple time if 3v3 loss is not a spike (we expect min 20ms low level for software drive shutdown) */
#define DCSTABWAITHRESHOLD      10      /* Waiting time after turning power supply on */
#define WAKEUPPULSETIME         10      /* Generated WakeUp pulse length */
#define SHUTDOWNPULSETIME       10      /* Generated ShutDown pulse length */
#define SHUTDOWNTIMEOUT         3000    /* Maximum time to wait after the shutdown pulse before interrupting the power supply (30s) */

#define TRUE    1
#define FALSE   0
typedef unsigned char boolean;

typedef enum _status {
    STATUS_SLEEP,
    STATUS_PON_REQ,
    STATUS_PULSE_WAKEUP_START,
    STATUS_CPU_PRE_RUNNING,
    STATUS_CPU_RUNNING,
    STATUS_CPU_LOSS3V3CHECK,
    STATUS_SHUTDOWN_START,
    STATUS_SHUTDOWN_REQ,
} status;

typedef struct _commondata {
    status curStatus;
    unsigned char stopPwOnCurTmr;
    unsigned char pwOnCurTmr;
    unsigned char preRunningTmr;
    unsigned char dcWaitTmr;
    unsigned char wakeUpTmr;
    unsigned char vLossTmr;
    unsigned char shutDownTmr;
    unsigned int shutDownTimeoutTmr;
} commondata;

commondata commonData;



static void turnPowerSupply(boolean on) {
    LATA2 = (on!=0);
}

static void setWakeUp(boolean on) {
    LATA5 = (on!=0);
}

static void setShutDown(boolean on) {
    LATA0 = (on!=0);
}

static boolean is3V3Present() {
    return ADC1_GetConversionResult() > V3VOLTAGETRESHOLD;
}

static void setNextStatus(status nextStatus) {
    commonData.curStatus = nextStatus;
}

// High-Endurance Flash Addresses
// 0380h-03FFh
#define HEF_ONSTATUS_VALUE    0x00
#define HEF_OFFSTATUS_VALUE   0xFF
#define HEF_POWERSTATUS_ADDRESS   0x0380

void unlockHEF (void) {
#asm
    BANKSEL PMCON2
    MOVLW   0x55
    MOVWF   PMCON2 & 0x7F
    MOVLW   0xAA
    MOVWF   PMCON2 & 0x7F
    BSF     PMCON1 & 0x7F, 1
    NOP
    NOP
#endasm
}

// Check if the system was ON when power loss occurred
// If yes, turn on the device
static void checkLastPowerStatus(void) {
    
    // Read HEF at HEF_POWERSTATUS_ADDRESS
    PMADR = HEF_POWERSTATUS_ADDRESS;
    PMCON1bits.CFGS = 0;
    PMCON1bits.RD = 1;
    NOP();
    NOP();
    
    unsigned char flashReadValue = PMDAT;
    if (flashReadValue == HEF_ONSTATUS_VALUE) {
        commonData.curStatus = STATUS_PON_REQ;
        commonData.stopPwOnCurTmr = 1;
    }
}

// Persists current system power status so it will be
// resumed after each power loss if any
static void saveLastPowerStatus(bool on) {
    
    INTERRUPT_GlobalInterruptDisable();
    
    // Erase Flash at HEF_POWERSTATUS_ADDRESS
    PMADR = HEF_POWERSTATUS_ADDRESS;
    PMCON1bits.CFGS = 0;
    PMCON1bits.FREE = 1;
    PMCON1bits.WREN = 1;
    unlockHEF();
    PMCON1bits.WREN = 0;
    
    if (on) {
        
        // Write HEF_ONSTATUS_VALUE at HEF_POWERSTATUS_ADDRESS
        PMADR = HEF_POWERSTATUS_ADDRESS;
        PMDAT = HEF_ONSTATUS_VALUE;
        PMCON1bits.LWLO = 0;
        PMCON1bits.CFGS = 0;
        PMCON1bits.FREE = 0;
        PMCON1bits.WREN = 1;
        unlockHEF();
        PMCON1bits.WREN = 0;
    }

    INTERRUPT_GlobalInterruptEnable();    
}

void InitApp() {

    // Low power sleep mode
    VREGCONbits.VREGPM = 1;

    setShutDown(FALSE);
    setWakeUp(FALSE);
    turnPowerSupply(FALSE);

    commonData.curStatus = STATUS_SLEEP;
    commonData.stopPwOnCurTmr = 0;
    commonData.pwOnCurTmr = 0;
    commonData.dcWaitTmr = 0;
    commonData.wakeUpTmr = 0;
    commonData.vLossTmr = 0;
    commonData.shutDownTmr = 0;
    commonData.shutDownTimeoutTmr = 0;
    
    // Check overall system power status
    // stored at previous run
    checkLastPowerStatus();

    // Initialize interrupts
    INTERRUPT_PeripheralInterruptEnable();
    INTERRUPT_GlobalInterruptEnable();
}

void Loop() {
    switch (commonData.curStatus) {

        case STATUS_SLEEP: {

            turnPowerSupply(FALSE);
            
            // If no pushbutton is pressed, go to sleep mode
            // It will wake up by the interrupt generated
            // by the power button
            if (commonData.pwOnCurTmr == 0) {
                SLEEP();
            }

            // Check if pushbutton has been pressed for the
            // required time to wake the system
            if (commonData.pwOnCurTmr > POWERONTHRESHOLD) {
                setNextStatus(STATUS_PON_REQ);
                commonData.pwOnCurTmr = 0;
                commonData.stopPwOnCurTmr = 1;
                commonData.dcWaitTmr = 0;
                
                // Save last power status as ON so the device
                // will start up automatically each power loss
                saveLastPowerStatus(true);
            }
        }
        break;

        case STATUS_PON_REQ: {

            // Enable the external power supply
            turnPowerSupply(TRUE);

            // Wait for power stabilization then check if the CPU needs a wakeup pulse
            if (commonData.dcWaitTmr > DCSTABWAITHRESHOLD) {

                // Check if wakeup pulse is required
                if (is3V3Present()) {
                    commonData.preRunningTmr = 0;
                    setNextStatus(STATUS_CPU_PRE_RUNNING);
                } else {
                    setWakeUp(TRUE);
                    setNextStatus(STATUS_PULSE_WAKEUP_START);
                    commonData.wakeUpTmr = 0;
                }
            }
        }
        break;

        case STATUS_PULSE_WAKEUP_START: {
            if (commonData.wakeUpTmr > WAKEUPPULSETIME) {
                commonData.preRunningTmr = 0;
                setNextStatus(STATUS_CPU_PRE_RUNNING);
            }
        }
        break;
        
        case STATUS_CPU_PRE_RUNNING: {
            setWakeUp(FALSE);
            
            // Wait some seconds before going to plain running
            if (commonData.preRunningTmr > PRERUNNINGTHRESHOLD) {
                setNextStatus(STATUS_CPU_RUNNING);
            }
        }
        break;

        case STATUS_CPU_RUNNING: {

            // Shutdown required by user
            if (commonData.pwOnCurTmr > SHUTDOWNTHRESHOLD) {
                setShutDown(TRUE);
                setNextStatus(STATUS_SHUTDOWN_START);
                commonData.pwOnCurTmr = 0;
                commonData.stopPwOnCurTmr = 1;
                commonData.shutDownTmr = 0;
                
                // Save last power status as OFF so the device
                // will not start up automatically each power loss
                saveLastPowerStatus(false);
            }
            
            // Shutdown triggered from CPU
            if (!is3V3Present()) {
                commonData.vLossTmr = 0;
                setNextStatus(STATUS_CPU_LOSS3V3CHECK);
            }
        }
        break;
        
        case STATUS_CPU_LOSS3V3CHECK:
        {
            if (commonData.vLossTmr >= V3VOLTAGELOSSTIMER) {
                
                if (!is3V3Present()) {
                    
                    // If 3v3 is still not present, turn off the device
                    setNextStatus(STATUS_SLEEP);
                } else {
                    
                    // If 3v3 is now present, go back to the running mode
                    setNextStatus(STATUS_CPU_RUNNING);
                }
                
                commonData.pwOnCurTmr = 0;
            }
        }
        break;

        case STATUS_SHUTDOWN_START: {
            if (commonData.shutDownTmr > SHUTDOWNPULSETIME) {
                setNextStatus(STATUS_SHUTDOWN_REQ);
                commonData.shutDownTimeoutTmr = 0;
            }
        }
        break;

        case STATUS_SHUTDOWN_REQ: {
            setShutDown(FALSE);
            if ((commonData.shutDownTimeoutTmr > SHUTDOWNTIMEOUT) || !is3V3Present()) {
                commonData.pwOnCurTmr = 0;
                setNextStatus(STATUS_SLEEP);
            }
        }
        break;

        default:
            commonData.pwOnCurTmr = 0;
            setNextStatus(STATUS_SLEEP);
            break;
    }
}

// Timer function:
void OnTimer() {

    if (POWERONPRESSED && (commonData.stopPwOnCurTmr == 0)) {
        INCCLAMPTOBYTE(commonData.pwOnCurTmr);        
    } else {
        commonData.stopPwOnCurTmr = 0;
        commonData.pwOnCurTmr = 0;
    }

    INCCLAMPTOBYTE(commonData.preRunningTmr);
    INCCLAMPTOBYTE(commonData.wakeUpTmr);
    INCCLAMPTOBYTE(commonData.dcWaitTmr);
    INCCLAMPTOBYTE(commonData.shutDownTmr);
    INCCLAMPTOBYTE(commonData.vLossTmr);
    INCCLAMPTOINT(commonData.shutDownTimeoutTmr);
}
