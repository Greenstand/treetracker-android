# Fix signup language selection timing

## Summary

Language selection now applies the selected locale as soon as the user taps a language option. The next button no longer re-applies the locale immediately before navigating, which avoids racing app locale recreation against signup navigation.
