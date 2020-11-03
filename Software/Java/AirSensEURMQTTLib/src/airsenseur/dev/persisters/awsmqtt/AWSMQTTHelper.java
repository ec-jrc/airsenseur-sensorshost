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

package airsenseur.dev.persisters.awsmqtt;

import airsenseur.dev.exceptions.PersisterException;
import com.amazonaws.services.iot.client.AWSIotDevice;
import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.cert.Certificate;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic wrapper for MQTT transactions with aws-iot-device-sdk client library
 * @author marco
 */
public class AWSMQTTHelper {
    
    private final String host;
    private final String clientID;

    private final PrivateKey privateKey;
    private final KeyStore keyStore;
    private final String keyPassword;
    
    private final Logger log = LoggerFactory.getLogger(AWSMQTTHelper.class);
    
    private AWSIotMqttClient awsIotClient;
    
    
    public AWSMQTTHelper(String host, String clientID, String keyFileName, String certFileName, String hostFileName, String keyAlgorithm) throws PersisterException {
        
        this.host = host;
        this.privateKey = loadPrivateKeyFromFile(keyFileName, keyAlgorithm);
        this.clientID = clientID;
        
        List<Certificate> certificates = loadCertificateFromFile(certFileName);
        List<Certificate> hostCertificates = null;
        if (!hostFileName.isEmpty()) {
            hostCertificates = loadCertificateFromFile(hostFileName);
        }
        
        try {
            this.keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            this.keyStore.load(null);
            this.keyPassword = new BigInteger(128, new SecureRandom()).toString(32);
            
            Certificate[] certChain = new Certificate[certificates.size()];
            certificates.toArray(certChain);
            this.keyStore.setKeyEntry("alias", this.privateKey, this.keyPassword.toCharArray(), certChain);
            //this.keyStore.setCertificateEntry("hostcert", cert);
            
            
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException ex) {
            throw new PersisterException("Failed to create keystore: " + ex.getMessage());
        }
    }
    
    public void openConnection(AWSIotDevice device) throws PersisterException {
        
        if (awsIotClient != null) {
            closeConnection();
        }
        
        awsIotClient = new AWSIotMqttClient(host, clientID, keyStore, keyPassword);
        try {
            //awsIotClient.attach(device);
            awsIotClient.connect();
            
            
            //device.delete();
            
        } catch (AWSIotException ex) {  
            throw new PersisterException(ex.getMessage());
        }
    }
    
    public void closeConnection() throws PersisterException {
        
        if (awsIotClient != null) {
            try {
                awsIotClient.disconnect();
            } catch (AWSIotException ex) {
                throw new PersisterException(ex.getMessage());
            }
        }
    }
    
    public boolean publishSample(String status) throws PersisterException {
        
        if (awsIotClient == null) {
            return false;
        }
        
        String topic = "$aws/things/"+clientID+"/shadow/update";
        try {
            awsIotClient.publish(topic, status);
        } catch (AWSIotException ex) {
            throw new PersisterException((ex.getMessage()));
        }
        
        return true;
        
    }
    
    // Load private key from file.
    // NOTE: This function only supports PKCS#8 file format. 
    // If you have a PKCS#1 file format, you can convert to PKCS#8 format by issuing an openssl command:
    // openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in privkey.pem -out privkey_pk8.pem
    private PrivateKey loadPrivateKeyFromFile(final String keyFileName, final String algorithm) throws PersisterException {
        
        log.info("Loading private Key from " + keyFileName);
        
        PrivateKey privKey = null;
        
        BufferedReader br; 
        try {
            br = new BufferedReader(new FileReader(keyFileName));
        } catch (FileNotFoundException e) {
            throw new PersisterException("File " + keyFileName + " not found");
        }
        String line;
        String strKeyPEM = "";
        try {
            while ((line = br.readLine()) != null) {
                strKeyPEM += line;
            }
            br.close();        
        } catch (IOException e) {
            throw new PersisterException("Error reading " + keyFileName + " file");
        }
        
        strKeyPEM = strKeyPEM.replace("-----BEGIN PRIVATE KEY-----", "");
        strKeyPEM = strKeyPEM.replace("-----END PRIVATE KEY-----", "");
        strKeyPEM = strKeyPEM.replace("\n", "");
        byte[] keyBytes = Base64.getDecoder().decode(strKeyPEM);
        
        try {
            
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance(algorithm);
            privKey = kf.generatePrivate(spec);
            
        } catch (GeneralSecurityException e) {
            throw new PersisterException(e.getMessage());
        }
        
        return privKey;
    } 
    
    private List<Certificate> loadCertificateFromFile(final String certFileName) throws PersisterException {
        
        log.info("Loading certificates from " + certFileName);
        
        File cFile = new File(certFileName);
        if (!cFile.exists()) {
            throw new PersisterException("File " + certFileName + " not found");
        }
        
        try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(cFile))) {
            final CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return (List<Certificate>) certFactory.generateCertificates(stream);
        } catch (IOException | CertificateException e) {
            throw new PersisterException("Error reading certificate file " + certFileName);
        }
    }
}
