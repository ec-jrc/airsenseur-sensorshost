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
 *                      European Commission - Joint Research Centre,
 * - Marco Signorini, marco.signorini@liberaintentio.com
 *
 * ===========================================================================
 */

#include <cstdlib>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <sys/types.h>

using namespace std;

#define BUFFERLENGTH    1024
#define MINIMUMVOLTAGE  2700
#define MINIMUMVOLTAGEMAXAGE 3
#define MINIMUMALERTVOLTAGEMAXAGE 12

#define EXTERNAL_SHUTDOWN_SCRIPT    "/etc/batterymonitor/lowvoltage_shutdown"
#define EXTERNAL_LOWBATT_SCRIPT     "/etc/batterymonitor/lowvoltage_alert"

char voltageFileName[BUFFERLENGTH];
char chargeFileName[BUFFERLENGTH];

long getVoltage();
bool getIsCharging();
void shutdownSystem();
void alertSystem();
void executeShellScript(const char* script, const char* pars);

/*
 * 
 */
int main(int argc, char** argv) {
    
    struct stat statRes;
    long voltageMin;
    long voltageAlert;
    unsigned char voltageTimer;
    unsigned char voltageAlertTimer;
    
    if (argc < 4) {
            printf("Usage: BatteryMonitor ltcXXXX ltcZZZZ KKKK YYYY\n");
            printf("Where ltcXXXX = battery gauge device name and id\n");
            printf("ltcZZZZ = battery manager device name and id\n");
            printf("KKKK is the alert battery voltage (in mV)\n");
            printf("And YYYY is the minimum battery voltage (in mV) for shutdown the system\n");
            return (EXIT_FAILURE);
    }

    // Generate proper voltage file name
    snprintf(voltageFileName, BUFFERLENGTH, "/sys/class/power_supply/%s/voltage_now", argv[argc - 4]);
    snprintf(chargeFileName, BUFFERLENGTH, "/sys/class/power_supply/%s/status", argv[argc-3]);
    voltageAlert = atol(argv[argc-2]);
    voltageMin = atol(argv[argc-1]);
    
    if (voltageMin < MINIMUMVOLTAGE) {
        printf("Warning: minimum required voltage is %d; continuing with this value.\n", MINIMUMVOLTAGE);
        voltageMin = MINIMUMVOLTAGE;
    }
    
    if (voltageAlert <= voltageMin) {
        voltageAlert = voltageMin + (voltageMin / 10);
        printf("Warning: alert voltage has been moved to %d\n", voltageAlert);
    }
    
    if (stat(voltageFileName, &statRes) < 0) {
        printf("Error opening the specified battery gauge device\n");
        return (EXIT_FAILURE);
    }
    
    if (stat(chargeFileName, &statRes) < 0) {
        printf("Error opening the specified battery manager device\n");
        return (EXIT_FAILURE);
    }
    
    if (stat(EXTERNAL_SHUTDOWN_SCRIPT, &statRes) < 0) {
        printf("To properly run we need to have the file %s where the shutdown procedure has been defined\n", EXTERNAL_SHUTDOWN_SCRIPT);
        return (EXIT_FAILURE);
    }
    
    if (stat(EXTERNAL_LOWBATT_SCRIPT, &statRes) < 0) {
        printf("To properly run we need to have the file %s where the shutdown procedure has been defined\n", EXTERNAL_LOWBATT_SCRIPT);
        return (EXIT_FAILURE);
    }

    voltageAlertTimer = 0;
    voltageTimer = 0;
    while (1) {
        
        // Check is charging
        bool isCharging = getIsCharging();
        
        // We don't need to check the battery voltage if the system is charging
        if (!isCharging) {
        
            // Read voltage
            int voltage = getVoltage();
            
            if (voltage != 0) {
                
                // Check voltage with alert threshold
                if (voltage < voltageAlert) {
                    
                    // voltage alert should still lower than required
                    // alert voltage for MINIMUMALERTVOLTAGEMAXAGE time
                    if (voltageAlertTimer == MINIMUMALERTVOLTAGEMAXAGE) {
                        
                        // Alert system
                        alertSystem();
                        
                        // This prevents alerting several times consecutively
                        voltageAlertTimer++;
                        
                    } else if (voltageAlertTimer < MINIMUMALERTVOLTAGEMAXAGE) {
                        voltageAlertTimer++;
                    }
                } else {
                    voltageAlertTimer = 0;
                }
                
                // Check voltage with minimum threshold
                if (voltage < voltageMin) {

                    // voltage should still lower than required
                    // minimum voltage for at least MINIMUMVOLTAGEMAXAGE time
                    voltageTimer++;
                    if (voltageTimer >= MINIMUMVOLTAGEMAXAGE) {

                        // Yes, voltage is too low for too much time.
                        shutdownSystem();

                        // Prevents continuously calling the script 
                        voltageTimer = 0;
                    }
                } else {
                    voltageTimer = 0;
                }
            }
        }
        sleep(10);
    }
    return 0;
}

long getVoltage() {
    
    FILE* fh;
    char c[BUFFERLENGTH];
    
    fh = fopen(voltageFileName, "r");
    if (fh != NULL) {
        
        fscanf(fh,"%[^\n]",c);
        fclose(fh);
        
        return atol(c)/1000;
    }

    printf("Error reading voltage\n");
    
    return 0;
}

bool getIsCharging() {
    FILE* fh;
    char c[BUFFERLENGTH];
    
    fh = fopen(chargeFileName, "r");
    if (fh != NULL) {
        
        fscanf(fh,"%[^\n]",c);
        fclose(fh);
        
        return (strcasecmp("charging", c) == 0);
    }

    printf("Error reading charging status\n");
    
    return 0;    
}

void shutdownSystem() {
    executeShellScript(EXTERNAL_SHUTDOWN_SCRIPT, "");
}

void alertSystem() {
    executeShellScript(EXTERNAL_LOWBATT_SCRIPT, "");
}

void executeShellScript(const char* script, const char* pars) {
    
    pid_t pid;
    int pidStatus;
    char scriptToRun[BUFFERLENGTH];
    char parameters[BUFFERLENGTH];
    
    strcpy(scriptToRun, script);
    strcpy(parameters, pars);
    if ((pid = fork()) == 0) {
            int res;

            res = execl(scriptToRun, parameters, (char *)NULL);
            if (-1 == res) exit(255);
    }

    wait(&pidStatus);        
}
