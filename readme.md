[![Build Status](https://travis-ci.org/theborakompanioni/mesqueteltra.svg?branch=master)](https://travis-ci.org/theborakompanioni/mesqueteltra)
mesqueteltra
====

### Build
```
gradlew clean build
```

### IPFS
```
ipfs daemon --enable-pubsub-experiment
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


