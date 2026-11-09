package ru.palma.app.ui

object Keys {
    const val BACKSPACE = 0xff08
    const val TAB = 0xff09
    const val RETURN = 0xff0d
    const val ESCAPE = 0xff1b
    const val DELETE = 0xffff
    const val HOME = 0xff50
    const val LEFT = 0xff51
    const val UP = 0xff52
    const val RIGHT = 0xff53
    const val DOWN = 0xff54
    const val PAGE_UP = 0xff55
    const val PAGE_DOWN = 0xff56
    const val END = 0xff57
    const val SHIFT = 0xffe1
    const val CONTROL = 0xffe3
    const val ALT = 0xffe9
    const val SUPER = 0xffeb

    private const val CYRILLIC_LOWER = "абцдефгхийклмнопярстужвьызшэщчъ"
    private const val CYRILLIC_A = 0x06c1
    private const val CYRILLIC_CAPITAL_A = 0x06e1
    private const val CYRILLIC_IO = 0x06a3
    private const val CYRILLIC_CAPITAL_IO = 0x06b3

    fun of(letter: Char): Int? = when {
        letter == 'ё' -> CYRILLIC_IO
        letter == 'Ё' -> CYRILLIC_CAPITAL_IO
        CYRILLIC_LOWER.indexOf(letter) >= 0 -> CYRILLIC_A + CYRILLIC_LOWER.indexOf(letter)
        CYRILLIC_LOWER.uppercase().indexOf(letter) >= 0 -> CYRILLIC_CAPITAL_A + CYRILLIC_LOWER.uppercase().indexOf(letter)
        letter.code in 0x20..0x7e -> letter.code
        letter == '\n' -> RETURN
        letter == '\t' -> TAB
        else -> null
    }
}
