// "Make bar internal" "false"
// ACTION: Add names to call arguments
// ACTION: Convert property initializer to getter
// ACTION: Convert to lazy property
// ACTION: Move to constructor
// ERROR: Cannot access 'bar': it is private in 'First'

private data class Data(val x: Int)

class First {
    // Making it internal exposes 'Data'
    private fun bar(x: Int) = Data(x)
}

class Second(f: First) {
    private val y = f.<caret>bar(42)
}
