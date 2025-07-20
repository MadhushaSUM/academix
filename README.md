## mTLS configuration

### Generate Certificates for Each Service

For each microservice, you'll perform the following steps:
* Generate a keystore for the service.
* Generate a Certificate Signing Request (CSR) for the service.
* Sign the CSR with your Root CA.
* Import the Root CA's public certificate into the service's keystore.
* Import the signed service certificate into the service's keystore.

1. Generate the service's key pair and keystore:
    ```
    keytool -genkeypair -alias <SERVICE_NAME> -keyalg RSA -keysize 2048 -validity 3650 -keystore <SERVICE_NAME>.jks -storepass changeit -keypass changeit -dname "CN=<SERVICE_NAME>, OU=Academix, O=Academix, L=Colombo, ST=Western, C=LK" -ext san=dns:<SERVICE_NAME>,ip:127.0.0.1
    ```
2. Generate a Certificate Signing Request (CSR) for the service:
    ```
   keytool -certreq -alias <SERVICE_NAME> -keystore <SERVICE_NAME>.jks -file <SERVICE_NAME>.csr -storepass changeit
   ```
3. Sign the service's CSR with the Root CA:
    ```
   keytool -gencert -alias rootca -keystore root-ca.jks -storepass changeit -infile <SERVICE_NAME>.csr -outfile <SERVICE_NAME>.cer -ext BasicConstraints:critical=ca:false -ext EKU=serverAuth,clientAuth -ext KeyUsage=digitalSignature,keyEncipherment -validity 3650
   ```
4. Import the Root CA's public certificate into the service's keystore:
    ```
   keytool -importcert -alias rootca -file root-ca.cer -keystore <SERVICE_NAME>.jks -storepass changeit -noprompt
   ```
5. Import the signed service certificate into its own keystore:
    ```
   keytool -importcert -alias <SERVICE_NAME> -file <SERVICE_NAME>.cer -keystore <SERVICE_NAME>.jks -storepass changeit -noprompt
   ```

### Create TrustStores for Each Service

1. Create a truststore and import the Root CA's public certificate:
    ```
   keytool -importcert -alias rootca -file root-ca.cer -keystore <SERVICE_NAME>-truststore.jks -storepass changeit -noprompt
   ```
