# Answer To Reset

Audience: SAS-UP FAE

## `T = 15` and interface byte

After `T = 15`, the first `tBi` byte is not arbitrary; it indicates interface byte.
For ETSI TS 102 221, the key values are:

- `0x82` (`0b1000_0010`): eUICC-related functions supported
- `0x81` (`0b1000_0001`): LSIs (Logical Secure Element Interfaces) supported

These are defined in [ETSI TS 102 221, clause 6.3.3, Table 6.7](https://www.etsi.org/deliver/etsi_ts/102200_102299/102221/18.02.00_60/ts_102221v180200p.pdf#page=36).

## Why it matters

- `T = 15` + first `tBi = 0x82`: the card is advertising **eUICC-related functions supported**. \
  On Android, removable eUICC with this flag is often treated as non-removable. \
  Result: the modem/Telephony stack may refuse to re-enable it as a normal SIM
- `T = 15` + first `tBi = 0x81`: the card is advertising **LSIs supported**. \
  This is not a strong compatibility signal for removable eUICC behavior. \
  Modem implementations vary; support is not consistently reliable.
