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

#include <stdlib.h>
#include <stdint.h>

#include <linux/input.h>

#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <sys/types.h>

#define SCRIPTPATH      "/etc/eventmonitor"
#define BUFFERLENGTH    256

// #define DEBUGPRINT(a, ...)      printf(a,  ##__VA_ARGS__)
#define DEBUGPRINT(a, ...)

int checkIsFile (const char *fileName);
void runShellScript (int code, int value);

/*
 * 
 */
int main(int argc, char** argv) {
    
    int fd, numRead, i;
    struct input_event evItems[32];
    
    
    if (argc < 2) {
            printf("Usage: EventMonitor /dev/input/eventX\n");
            printf("Where X = input device number\n");
            return 1;
    }
    
    if ((fd = open(argv[argc - 1], O_RDONLY)) < 0) {
            printf("Error opening the specified input device\n");
            return (EXIT_FAILURE);
    }
    
    /* Loop forever, reading and filtering keyboard events */
    while (1) {
        
        /* Read as much events you can (up to 32) */
        numRead = read(fd, evItems, sizeof(struct input_event) * 32);
        
        if (numRead >= (int) sizeof(struct input_event)) {

            // Process all read events.
            for (i = 0; i < numRead / sizeof(struct input_event); i++) {
                if (evItems[i].type == EV_KEY) {
                    runShellScript(evItems[i].code, evItems[i].value);
                }
            }
        }
    }
    
    return (EXIT_SUCCESS);
}

// Check for a file validity
int checkIsFile (const char *path) {
        struct stat buffer;
        
        if (stat(path, &buffer) == -1) {
            return 0;
        };

        return S_ISREG(buffer.st_mode);
}


// Run a shell script handler
void runShellScript (int code, int value) {
        char codeString[BUFFERLENGTH];
        char valueString[8];
        char *scriptFullName;
        
        pid_t pid;
        int pidStatus;

        scriptFullName = (char *)malloc(BUFFERLENGTH);
        snprintf(scriptFullName, BUFFERLENGTH, "%s/handler_%d", SCRIPTPATH, code);
        
        DEBUGPRINT("Trying running the script %s\n", scriptFullName);
        if (!checkIsFile(scriptFullName)) {
            DEBUGPRINT("The script %s does not exist\n", scriptFullName);
            free(scriptFullName);
            return;
        }

        snprintf(codeString, BUFFERLENGTH, "%d", code);
        sprintf(valueString, "%d", value);

        DEBUGPRINT("pin %d: running script %s\n", code, scriptFullName);

        if ((pid = fork()) == 0) {
                int res;
                
                res = execl(scriptFullName, codeString, valueString, (char *)NULL);
                if (-1 == res) exit(255);
        }

        wait(&pidStatus);

        free(scriptFullName);
}                