[![Build Status](https://travis-ci.org/theborakompanioni/mesqueteltra.svg?branch=master)](https://travis-ci.org/theborakompanioni/mesqueteltra)
mesqueteltra
====

### Build
Make sure you have [Java Cryptography Extension (JCE)](http://www.oracle.com/technetwork/java/javase/downloads/index.html) installed.

Run command:
```
gradlew clean build
```

### MQTT
Starts a MQTT broker on port `18881` by default.
It includes an example keystore including a self signed certificate with password `example`.
```
keytool -genkey -keyalg RSA -alias selfsigned -keystore example_keystore.jks -storepass example -validity 99999 -keysize 2048
```
and a client example keystore that trusts the self signed certificate:
```
keytool -importkeystore -srckeystore example_keystore.jks -destkeystore example_keystore.p12 -srcstoretype jks -deststoretype pkcs12
openssl pkcs12 -in example_keystore.p12 -out example_keystore.pem
openssl x509 -outform der -in example_keystore.pem -out example_keystore.der
keytool -import -keystore client_example_keystore.jks -file example_keystore.der
```


### IPFS
```
ipfs daemon --enable-pubsub-experiment --enable-gc &
```


#### create local tunnel to ipfs server
```
ssh -L 5001:localhost:5001 -L 4001:localhost:4001 -L 8080:localhost:8080  user@remote-ip
```

e.g. if your user is `myuser` and your ssh daemon runs on port `1022` on machine `192.168.1.50`
```
ssh -L 5001:localhost:5001 -L 4001:localhost:4001 -L 8080:localhost:8080  myuser@192.168.1.50 -p 1022
```

See IPFS messages on topic '/time': http://127.0.0.1:5001/api/v0/pubsub/sub?arg=/time

See "Hello World!" file with has `QmfM2r8seH2GiRaC4esTjeraXEachRt8ZsSeGaWTPLyMoG` on:
http://localhost:8080/ipfs/QmfM2r8seH2GiRaC4esTjeraXEachRt8ZsSeGaWTPLyMoG


