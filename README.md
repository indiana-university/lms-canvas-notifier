# ess-lms-canvas-canvas-notifier

To Debug w/ Intellij, forward 5005 (in kube-forwarder, or k9s) to any desired port and then hook intellij up to that

```
helm upgrade canvasnotifier harbor-prd/k8s-boot -f helm-common.yaml -f helm-dev.yaml --install
```

```
helm upgrade camvasnotifier harbor-prd/k8s-boot -f helm-common.yaml -f helm-snd.yaml --install
```

```
helm upgrade canvasnotifier-expire-elevations ../k8s --values helm-common.yaml,helm-dev.yaml,helm-canvasnotifier-expire-elevations.yaml,../helm-vault-local.yaml --install
helm upgrade canvasnotifier-reg-expire-elevations ../k8s --values helm-common.yaml,helm-reg.yaml,helm-canvasnotifier-expire-elevations.yaml --install -n ua-vpit--enterprise-systems--lms--helm-release
helm upgrade canvasnotifier-stg-expire-elevations ../k8s --values helm-common.yaml,helm-stg.yaml,helm-canvasnotifier-expire-elevations.yaml --install -n ua-vpit--enterprise-systems--lms--helm-release
helm upgrade canvasnotifier-prd-expire-elevations ../k8s --values helm-common.yaml,helm-prd.yaml,helm-canvasnotifier-expire-elevations.yaml --install -n ua-vpit--enterprise-systems--lms--helm-release
```