package bose.ankush.auth.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoginValidationTest {
    @Test
    fun emailIsCheckedFirst() {
        assertEquals(LoginInputError.EMAIL_EMPTY, validateLogin(" ", ""))
        assertEquals(LoginInputError.EMAIL_INVALID, validateLogin("ankush@", "secret1"))
        assertEquals(LoginInputError.EMAIL_INVALID, validateLogin("ankush.example.com", "secret1"))
    }

    @Test
    fun passwordNeedsSixCharacters() {
        assertEquals(LoginInputError.PASSWORD_EMPTY, validateLogin("a@b.in", ""))
        assertEquals(LoginInputError.PASSWORD_SHORT, validateLogin("a@b.in", "12345"))
    }

    @Test
    fun validFormPasses() {
        assertNull(validateLogin("a@b.in", "123456"))
        // Keyboards often add a trailing space after an email suggestion.
        assertNull(validateLogin("a@b.in ", "123456"))
    }
}
