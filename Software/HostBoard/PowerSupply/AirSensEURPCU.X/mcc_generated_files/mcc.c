/**
  @Generated MPLAB� Code Configurator Source File

  @Company:
    Microchip Technology Inc.

  @File Name:
    mcc.c

  @Summary:
    This is the mcc.c file generated using MPLAB� Code Configurator

  @Description:
    This header file provides implementations for driver APIs for all modules selected in the GUI.
    Generation Information :
        Product Revision  :  MPLAB� Code Configurator - v2.25.2
        Device            :  PIC12F1571
        Driver Version    :  1.02
    The generated drivers are tested against the following:
        Compiler          :  XC8 v1.34
        MPLAB             :  MPLAB X v2.35 or v3.00
 */

/*
Copyright (c) 2013 - 2015 released Microchip Technology Inc.  All rights reserved.

Microchip licenses to you the right to use, modify, copy and distribute
Software only when embedded on a Microchip microcontroller or digital signal
controller that is integrated into your product or third party product
(pursuant to the sublicense terms in the accompanying license agreement).

You should refer to the license agreement accompanying this Software for
additional information regarding your rights and obligations.

SOFTWARE AND DOCUMENTATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND,
EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF
MERCHANTABILITY, TITLE, NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE.
IN NO EVENT SHALL MICROCHIP OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER
CONTRACT, NEGLIGENCE, STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR
OTHER LEGAL EQUITABLE THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES
INCLUDING BUT NOT LIMITED TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR
CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF PROCUREMENT OF
SUBSTITUTE GOODS, TECHNOLOGY, SERVICES, OR ANY CLAIMS BY THIRD PARTIES
(INCLUDING BUT NOT LIMITED TO ANY DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 */

// Configuration bits: selected in the GUI

// CONFIG1
#pragma config BOREN = OFF    // Brown-out Reset Enable->Brown-out Reset disabled
#pragma config PWRTE = ON    // Power-up Timer Enable->PWRT enabled
#pragma config FOSC = INTOSC    // ->INTOSC oscillator; I/O function on CLKIN pin
#pragma config MCLRE = OFF    // MCLR Pin Function Select->MCLR/VPP pin function is digital input
#pragma config CP = ON    // Flash Program Memory Code Protection->Program memory code protection is enabled
#pragma config WDTE = OFF    // Watchdog Timer Enable->WDT disabled
#pragma config CLKOUTEN = OFF    // Clock Out Enable->CLKOUT function is disabled. I/O or oscillator function on the CLKOUT pin

// CONFIG2
#pragma config WRT = ALL    // Flash Memory Self-Write Protection->000h to 7FFh write protected, no addresses may be modified by EECON control
#pragma config LPBOREN = OFF    // Low Power Brown-out Reset enable bit->LPBOR is disabled
#pragma config LVP = OFF    // Low-Voltage Programming Enable->High-voltage on MCLR/VPP must be used for programming
#pragma config STVREN = ON    // Stack Overflow/Underflow Reset Enable->Stack Overflow or Underflow will cause a Reset
#pragma config PLLEN = OFF    // PLL Enable->4x PLL disabled
#pragma config BORV = LO    // Brown-out Reset Voltage Selection->Brown-out Reset Voltage (Vbor), low trip point selected.

#include "mcc.h"

void SYSTEM_Initialize(void) {
    OSCILLATOR_Initialize();
    PIN_MANAGER_Initialize();
    ADC1_Initialize();
    FVR_Initialize();
    TMR0_Initialize();
    TMR2_Initialize();
}

void OSCILLATOR_Initialize(void) {
    // SPLLEN disabled; SCS INTOSC; IRCF 1MHz_HF; 
    OSCCON = 0x5A;
    // OSTS intosc; HFIOFR disabled; HFIOFS not0.5percent_acc; PLLR disabled; MFIOFR disabled; HFIOFL not2percent_acc; LFIOFR disabled; 
    OSCSTAT = 0x00;
    // TUN 0x0; 
    OSCTUNE = 0x00;
    // Set the secondary oscillator

}

/**
 End of File
 */