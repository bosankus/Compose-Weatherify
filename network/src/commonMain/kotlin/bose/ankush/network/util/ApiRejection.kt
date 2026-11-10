package bose.ankush.network.util

/**
 * The server answered and refused the request (`status: false` in the envelope). Unlike a
 * transport failure, [message] is the server's own reason, written for the user, so it is safe to
 * show — for example a photo that is too large or in a format it does not accept.
 */
class ApiRejection(
    override val message: String,
) : Exception(message)
