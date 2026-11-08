# ARA-M Applet Configuration

Audience: SAS-UP FAE

This configuration affects only EasyEUICC and is unrelated to OpenEUICC.

The ARA-M applet configuration is **REQUIRED** for EasyEUICC to communicate with the card in the device.

The current EasyEUICC signing key is valid until 24 Dec 2043 and is represented by the following fingerprints:

- `2A2FA878BC7C3354C2CF82935A5945A3EDAE4AFA` (SHA-1, **PREFERRED**)
- `57BECC26B373A6475D093D33B0EA1C5E2BF2738EC0233680094CCBBCB500A13E` (SHA-256)

## Signing key rotation policy

There is currently no formal rotation plan or contingency. The design prioritizes maximum compatibility while a rotation strategy is selected in 2038 and implemented no later than 24 Dec 2038.

This approach provides a five-year migration window before the current signing key expires on 24 Dec 2043.

## References

- [UICC carrier privileges](https://source.android.com/docs/core/connect/uicc#rules_on_uicc)
