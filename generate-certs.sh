#!/bin/bash

set -e

CA_DIR=certs/ca
SERVICES=("api-gateway" "user-management-service" "course-management-service")
PASSWORD=changeit
KEYSTORE_PASSWORD=$PASSWORD
TRUSTSTORE_PASSWORD=$PASSWORD

rm -rf certs
mkdir -p "$CA_DIR"

echo "🔧 Generating Root CA..."
openssl genrsa -out $CA_DIR/ca.key 4096
openssl req -x509 -new -nodes -key $CA_DIR/ca.key -sha256 -days 3650 -out $CA_DIR/ca.crt -subj "/CN=AcademixCA"

for SERVICE in "${SERVICES[@]}"
do
  SERVICE_DIR=certs/$SERVICE
  mkdir -p $SERVICE_DIR

  echo "🔧 Generating key and cert for $SERVICE..."

  # Create config file with SANs
  cat > $SERVICE_DIR/$SERVICE.conf << EOF
[ req ]
default_bits = 2048
distinguished_name = req_distinguished_name
req_extensions = v3_req
prompt = no

[ req_distinguished_name ]
CN = $SERVICE

[ v3_req ]
basicConstraints = CA:FALSE
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth, clientAuth
subjectAltName = @alt_names

[ alt_names ]
DNS.1 = $SERVICE
DNS.2 = localhost
DNS.3 = 127.0.0.1
DNS.4 = host.docker.internal
EOF

  openssl genrsa -out $SERVICE_DIR/$SERVICE.key 2048
  openssl req -new -key $SERVICE_DIR/$SERVICE.key -out $SERVICE_DIR/$SERVICE.csr -config $SERVICE_DIR/$SERVICE.conf -extensions v3_req
  openssl x509 -req -in $SERVICE_DIR/$SERVICE.csr -CA $CA_DIR/ca.crt -CAkey $CA_DIR/ca.key -CAcreateserial -out $SERVICE_DIR/$SERVICE.crt -days 365 -sha256 -extensions v3_req -extfile $SERVICE_DIR/$SERVICE.conf

  echo "📦 Creating PKCS12 keystore for $SERVICE..."
  openssl pkcs12 -export \
    -in $SERVICE_DIR/$SERVICE.crt \
    -inkey $SERVICE_DIR/$SERVICE.key \
    -out $SERVICE_DIR/$SERVICE.p12 \
    -name $SERVICE \
    -CAfile $CA_DIR/ca.crt \
    -caname rootCA \
    -passout pass:$KEYSTORE_PASSWORD

  echo "🛡️ Creating truststore for $SERVICE..."
  keytool -import -noprompt \
    -alias rootCA \
    -file $CA_DIR/ca.crt \
    -keystore $SERVICE_DIR/truststore.p12 \
    -storetype PKCS12 \
    -storepass $TRUSTSTORE_PASSWORD
done

echo "✅ Done. Keystores and truststores generated under ./certs/"