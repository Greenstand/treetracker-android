package org.greenstand.android.TreeTracker.utilities

import java.util.regex.Pattern

object Validation {

    fun isEmailValid(email: String): Boolean {
        return Pattern.compile(
                "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]|[\\w-]{2,}))@"
                        + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                        + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                        + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                        + "[0-9]{1,2}|25[0-5]|2[0-4][0-9]))|"
                        + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$"
        ).matcher(email).matches()
    }

    fun isValidPhoneNumber(phoneNumber: String): Boolean {

        val phoneNumberPattern = Pattern.compile("\\d{7,15}")

        val cleanPhoneNumber = cleanPhoneNumber(phoneNumber)

        return phoneNumberPattern.matcher(cleanPhoneNumber).matches()
    }

    fun cleanPhoneNumber(phoneNumber: String): String {
        phoneNumber.replace("+", "")
        phoneNumber.replace(" ", "")
        phoneNumber.replace("(", "")
        phoneNumber.replace(")", "")
        phoneNumber.replace("-", "")
        return phoneNumber
    }


}