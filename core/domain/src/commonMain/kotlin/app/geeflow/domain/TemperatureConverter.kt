@file:Suppress("MagicNumber")

package app.geeflow.domain

fun Float.celsiusToFahrenheit(): Float = this * 9f / 5f + 32f

fun Float.fahrenheitToCelsius(): Float = (this - 32f) * 5f / 9f
