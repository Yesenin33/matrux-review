package ru.palma.app.learn

object Fixture {
    val json = """
        {
          "id": "demo",
          "title": "Демо",
          "outcome": "Проверяешь разбор",
          "note": "Без запуска",
          "modules": [
            {
              "id": "m1",
              "title": "Первый",
              "lessons": [
                {
                  "id": "a",
                  "title": "Первый урок",
                  "theory": ["Один", "Два"],
                  "example": {"lang": "python", "code": "print(1)"},
                  "task": {"kind": "run", "prompt": "Выведи 2", "runtime": "python", "starter": "", "solution": "print(2)\n", "expect": "2", "stdin": "x\n"},
                  "review": "Разбор"
                },
                {
                  "id": "b",
                  "title": "Второй урок",
                  "theory": ["Один", "Два"],
                  "example": {"lang": "html", "code": "<p>1</p>"},
                  "task": {"kind": "run", "prompt": "Абзац", "runtime": "web", "solution": "<p>1</p>", "check": "!!document.querySelector('p')"},
                  "review": "Разбор"
                }
              ]
            },
            {
              "id": "m2",
              "title": "Второй",
              "lessons": [
                {
                  "id": "c",
                  "title": "Третий урок",
                  "theory": ["Один", "Два", "Три"],
                  "example": {"lang": "go", "code": "fmt.Println(1)"},
                  "task": {"kind": "choice", "prompt": "Что верно", "options": ["x", "y", "z"], "answer": 1, "code": {"lang": "go", "code": "x := 1"}},
                  "review": "Разбор"
                },
                {
                  "id": "d",
                  "title": "Четвёртый урок",
                  "theory": ["Один", "Два"],
                  "example": {"lang": "cpp", "code": "int x = 1;"},
                  "task": {"kind": "output", "prompt": "Что напечатает", "code": {"lang": "cpp", "code": "std::cout << 1;"}, "answer": "1\n2"},
                  "review": "Разбор"
                }
              ]
            }
          ]
        }
    """.trimIndent()

    val course: Course by lazy { CourseParser.course(json) }
}
