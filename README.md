# P2019
FRI 2019: Prevajalniki

## Project Structure
The main class is under src/compiler/Main.java

The common package has code that is shared between different phases.  It also contains a Report.java that is used for error reporting to the user.
The data package contains the Symbol class with defined symbols recognized by Lexical Analyzer and used by other phases.
The phases package contains different phases of the compiler (Lexical Analiysis, Syntax Analiysis, etc.). Each of the phases is in its own subpackage. Results of every phase are logged in a file. This is the only package that can be modified since we will get more code that relies on it for other assignments.

## Running the compiler
Makefiles can be used or run by java commands. The commands are scanned in Main.java.

The output is generated as an .xml file and .html file that displays the xml output in a nice way. Open it in a browser. XML files are the required output for the assignment checking.

To run Lexan phase:
test.txt --target-phase=lexan --logged-phase=lexan

## Phases
All phases terminate with first error. 

### LexAn
@ is ADDR
  is DATA

To get Symbols from the LexAn use the lexer() method from LexAn.java to get the next symbol from the source file.

### SynAn

We have to transform the ambigious CFG provided into a LL(1) compatible grammar. 

Example:
```
E -> num
E -> E + E | E - E | E \* E | E / E |
E -> ( E )
```
To make + left associative:
```
T -> T + F
F -> num
```
Priority:
E describes expressions with +,-,\*,/ or no operator as the top operator.
T describes expressions with \*,/ or no operator as the top operator.
F describes expressions with no operator as the top operator.
```
E -> E + T | E - T | T
T -> T \* F | T / F | F
F -> num
```
Which would turn 1\*2+3 into derivation E -> E+T -> T+T -> T\*F+T -> F\*F+T -> 1\*F+T -> 1\*2+T -> 1\*2+F -> 1\*2+3. Even though this is unambigious it is still not LL(1).
The problem is the left recursion. To get rid of it, we add E' (as an example).
```
E -> T E'
E' -> + T E' | - T E' | eps
```
You can also use the tool at: http://mdaines.github.io/grammophone/ -> input your cfg -> transform -> remove left recursion.

## Notes:
- System.out.println is not allowed for grading, should be turned off before turning in any of the assignments.
- For assignments we can only use basic JDK packages.
- For assignment turn ins, the file must be named properly (student id, etc.) and the project structure must be the same as of the provided files. The zip must unzip into directory called prev and then containing the packages.
