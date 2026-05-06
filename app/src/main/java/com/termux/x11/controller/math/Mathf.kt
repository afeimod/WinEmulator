package com.termux.x11.controller.math

abstract class Mathf {
    companion object {
        fun clamp(x: Float, min: Float, max: Float): Float {
            return if (x < min) min else if (x > max) max else x
        }

        fun clamp(x: Int, min: Int, max: Int): Int {
            return if (x < min) min else if (x > max) max else x
        }

        fun roundTo(x: Float, step: Float): Float {
            return (Math.floor((x / step).toDouble()) * step).toFloat()
        }

        fun roundPoint(x: Float): Int {
            return if (x <= 0) Math.floor(x.toDouble()).toInt() else Math.ceil(x.toDouble()).toInt()
        }

        fun sign(x: Float): Byte {
            return if (x < 0) -1 else if (x > 0) 1 else 0
        }

        fun lengthSq(x: Float, y: Float): Float {
            return x * x + y * y
        }
    }
}
