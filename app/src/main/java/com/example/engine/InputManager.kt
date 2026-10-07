package com.example.engine

class InputManager {
    @Volatile
    var touchLeft: Boolean = false
        private set

    @Volatile
    var touchRight: Boolean = false
        private set

    @Volatile
    var touchBoost: Boolean = false
        private set

    @Volatile
    var keyLeft: Boolean = false
        private set

    @Volatile
    var keyRight: Boolean = false
        private set

    @Volatile
    var keyBoost: Boolean = false
        private set

    @Volatile
    private var boostJustPressed: Boolean = false

    val moveLeft: Boolean
        get() = touchLeft || keyLeft

    val moveRight: Boolean
        get() = touchRight || keyRight

    val boostHeld: Boolean
        get() = touchBoost || keyBoost

    fun setTouchLeftState(pressed: Boolean) {
        touchLeft = pressed
    }

    fun setTouchRightState(pressed: Boolean) {
        touchRight = pressed
    }

    fun setTouchBoostState(pressed: Boolean) {
        if (pressed && !touchBoost) {
            boostJustPressed = true
        }
        touchBoost = pressed
    }

    fun setKeyLeftState(pressed: Boolean) {
        keyLeft = pressed
    }

    fun setKeyRightState(pressed: Boolean) {
        keyRight = pressed
    }

    fun setKeyBoostState(pressed: Boolean) {
        if (pressed && !keyBoost) {
            boostJustPressed = true
        }
        keyBoost = pressed
    }

    fun consumeBoostJustPressed(): Boolean {
        val wasPressed = boostJustPressed
        boostJustPressed = false
        return wasPressed
    }

    fun clearAll() {
        touchLeft = false
        touchRight = false
        touchBoost = false
        keyLeft = false
        keyRight = false
        keyBoost = false
        boostJustPressed = false
    }
}
