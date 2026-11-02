package com.example.engine

data class CppTemplate(
    val id: String,
    val title: String,
    val description: String,
    val category: String, // Game, Tool, Math, RPG, Learning
    val iconKey: String,
    val colorHex: String,
    val defaultDisplayMode: String, // "TERMINAL" or "GUI_FORM"
    val code: String
)

object CppTemplates {
    val templates = listOf(
        CppTemplate(
            id = "number_guesser",
            title = "Number Guessing Game",
            description = "Guess the secret number between 1 and 100 with interactive hints and score tracker.",
            category = "Game",
            iconKey = "sports_esports",
            colorHex = "#00D4FF",
            defaultDisplayMode = "TERMINAL",
            code = """
#include <iostream>
#include <cstdlib>
#include <ctime>

using namespace std;

int main() {
    cout << "======================================" << endl;
    cout << "       NUMBER GUESSING CHALLENGE      " << endl;
    cout << "======================================" << endl;
    cout << "I have chosen a secret number between 1 and 100." << endl;
    
    int secret = (rand() % 100) + 1;
    int guess = 0;
    int attempts = 0;
    int maxAttempts = 7;
    
    cout << "You have " << maxAttempts << " attempts. Good luck!" << endl << endl;
    
    while (guess != secret && attempts < maxAttempts) {
        attempts = attempts + 1;
        cout << "[Attempt " << attempts << "/" << maxAttempts << "] Enter your guess: ";
        cin >> guess;
        
        if (guess == secret) {
            cout << endl << ">>> BINGO! You guessed the secret number " << secret << " in " << attempts << " tries! <<<" << endl;
            break;
        } else if (guess < secret) {
            cout << "-> Too LOW! Try a higher number." << endl;
        } else {
            cout << "-> Too HIGH! Try a lower number." << endl;
        }
    }
    
    if (guess != secret) {
        cout << endl << "GAME OVER! The secret number was: " << secret << endl;
    }
    
    cout << "Thanks for playing!" << endl;
    return 0;
}
""".trimIndent()
        ),

        CppTemplate(
            id = "cli_calculator",
            title = "Terminal Calculator",
            description = "Multi-operation calculator with support for arithmetic, powers, and roots.",
            category = "Tool",
            iconKey = "calculate",
            colorHex = "#10B981",
            defaultDisplayMode = "TERMINAL",
            code = """
#include <iostream>
#include <cmath>

using namespace std;

int main() {
    cout << "=============================" << endl;
    cout << "      C++ CLI CALCULATOR     " << endl;
    cout << "=============================" << endl;
    cout << "1. Addition (+)" << endl;
    cout << "2. Subtraction (-)" << endl;
    cout << "3. Multiplication (*)" << endl;
    cout << "4. Division (/)" << endl;
    cout << "5. Power (a^b)" << endl;
    cout << "6. Square Root (sqrt(a))" << endl;
    cout << "-----------------------------" << endl;
    
    int choice;
    cout << "Select operation (1-6): ";
    cin >> choice;
    
    if (choice >= 1 && choice <= 5) {
        double num1, num2;
        cout << "Enter first number: ";
        cin >> num1;
        cout << "Enter second number: ";
        cin >> num2;
        
        if (choice == 1) {
            cout << "Result: " << num1 << " + " << num2 << " = " << (num1 + num2) << endl;
        } else if (choice == 2) {
            cout << "Result: " << num1 << " - " << num2 << " = " << (num1 - num2) << endl;
        } else if (choice == 3) {
            cout << "Result: " << num1 << " * " << num2 << " = " << (num1 * num2) << endl;
        } else if (choice == 4) {
            if (num2 != 0) {
                cout << "Result: " << num1 << " / " << num2 << " = " << (num1 / num2) << endl;
            } else {
                cout << "Error: Division by zero is undefined!" << endl;
            }
        } else if (choice == 5) {
            cout << "Result: " << num1 << " ^ " << num2 << " = " << pow(num1, num2) << endl;
        }
    } else if (choice == 6) {
        double num;
        cout << "Enter positive number: ";
        cin >> num;
        if (num >= 0) {
            cout << "Result: sqrt(" << num << ") = " << sqrt(num) << endl;
        } else {
            cout << "Error: Cannot calculate square root of negative number!" << endl;
        }
    } else {
        cout << "Invalid selection! Please restart." << endl;
    }
    
    return 0;
}
""".trimIndent()
        ),

        CppTemplate(
            id = "text_rpg",
            title = "Dungeon Quest RPG",
            description = "Interactive text-based dungeon crawler with inventory, combat, and dice rolls.",
            category = "Game",
            iconKey = "shield",
            colorHex = "#F59E0B",
            defaultDisplayMode = "TERMINAL",
            code = """
#include <iostream>
#include <string>
#include <cstdlib>

using namespace std;

int main() {
    cout << "****************************************" << endl;
    cout << "*       WELCOME TO DUNGEON QUEST       *" << endl;
    cout << "****************************************" << endl;
    
    string heroName;
    cout << "Brave adventurer, enter your name: ";
    cin >> heroName;
    
    cout << "Greetings, " << heroName << "! Choose your class:" << endl;
    cout << "1. Warrior (HP: 100, Atk: 15)" << endl;
    cout << "2. Mage (HP: 75, Atk: 22)" << endl;
    cout << "3. Rogue (HP: 85, Atk: 18)" << endl;
    
    int classChoice;
    cout << "Select class (1-3): ";
    cin >> classChoice;
    
    int heroHp = 100;
    int heroAtk = 15;
    string className = "Warrior";
    
    if (classChoice == 2) {
        heroHp = 75;
        heroAtk = 22;
        className = "Mage";
    } else if (classChoice == 3) {
        heroHp = 85;
        heroAtk = 18;
        className = "Rogue";
    }
    
    cout << endl << heroName << " the " << className << " enters the Gloom Dungeon..." << endl;
    cout << "A fearsome Goblin Captain appears! (HP: 60)" << endl;
    
    int monsterHp = 60;
    int potions = 2;
    int turn = 1;
    
    while (heroHp > 0 && monsterHp > 0) {
        cout << endl << "--- Turn " << turn << " | " << heroName << " HP: " << heroHp << " | Goblin HP: " << monsterHp << " | Potions: " << potions << " ---" << endl;
        cout << "1. Attack with weapon" << endl;
        cout << "2. Drink Health Potion (+30 HP)" << endl;
        cout << "3. Defend (Reduce incoming damage)" << endl;
        
        int action;
        cout << "Choose action (1-3): ";
        cin >> action;
        
        if (action == 1) {
            int dmg = heroAtk + (rand() % 6);
            monsterHp = monsterHp - dmg;
            cout << ">> You strike the Goblin for " << dmg << " damage!" << endl;
        } else if (action == 2) {
            if (potions > 0) {
                heroHp = heroHp + 30;
                potions = potions - 1;
                cout << ">> You drank a potion! Restored 30 HP." << endl;
            } else {
                cout << ">> No potions remaining!" << endl;
            }
        } else {
            cout << ">> You raise your guard defensively!" << endl;
        }
        
        if (monsterHp <= 0) {
            cout << endl << "VICTORY! You defeated the Goblin Captain!" << endl;
            cout << "You found 150 gold coins and escaped safely!" << endl;
            break;
        }
        
        int monsterDmg = 10 + (rand() % 8);
        if (action == 3) {
            monsterDmg = monsterDmg / 2;
            cout << ">> Your defense blocked half the blow!" << endl;
        }
        heroHp = heroHp - monsterDmg;
        cout << ">> The Goblin slashes you for " << monsterDmg << " damage!" << endl;
        
        if (heroHp <= 0) {
            cout << endl << "DEFEAT! You were slain in the dungeon depths..." << endl;
            break;
        }
        turn = turn + 1;
    }
    
    return 0;
}
""".trimIndent()
        ),

        CppTemplate(
            id = "grade_analyzer",
            title = "Student Grade Analyzer",
            description = "Compute class statistics: average scores, highest, lowest, and letter grades.",
            category = "Productivity",
            iconKey = "school",
            colorHex = "#8B5CF6",
            defaultDisplayMode = "GUI_FORM",
            code = """
#include <iostream>
#include <string>

using namespace std;

int main() {
    cout << "========================================" << endl;
    cout << "      STUDENT GRADE PERFORMANCE TOOL    " << endl;
    cout << "========================================" << endl;
    
    int count;
    cout << "Enter number of students (1-10): ";
    cin >> count;
    
    if (count <= 0 || count > 10) {
        cout << "Invalid count! Setting to 3." << endl;
        count = 3;
    }
    
    double total = 0;
    double highest = -1;
    double lowest = 101;
    string topStudent = "";
    
    for (int i = 1; i <= count; i = i + 1) {
        string name;
        double score;
        cout << endl << "Enter Student #" << i << " name: ";
        cin >> name;
        cout << "Enter score (0-100) for " << name << ": ";
        cin >> score;
        
        total = total + score;
        
        if (score > highest) {
            highest = score;
            topStudent = name;
        }
        if (score < lowest) {
            lowest = score;
        }
        
        string grade = "F";
        if (score >= 90) grade = "A";
        else if (score >= 80) grade = "B";
        else if (score >= 70) grade = "C";
        else if (score >= 60) grade = "D";
        
        cout << "-> " << name << ": " << score << "% (" << grade << ")" << endl;
    }
    
    double average = total / count;
    cout << endl << "----------------------------------------" << endl;
    cout << "CLASS SUMMARY:" << endl;
    cout << "Total Students: " << count << endl;
    cout << "Class Average:  " << average << "%" << endl;
    cout << "Highest Score:  " << highest << "% (" << topStudent << ")" << endl;
    cout << "Lowest Score:   " << lowest << "%" << endl;
    cout << "----------------------------------------" << endl;
    
    return 0;
}
""".trimIndent()
        ),

        CppTemplate(
            id = "unit_converter",
            title = "Unit & Metric Converter",
            description = "Convert Temperature (C/F/K), Length (km/mi), and Weight (kg/lb).",
            category = "Utility",
            iconKey = "swap_horiz",
            colorHex = "#EC4899",
            defaultDisplayMode = "GUI_FORM",
            code = """
#include <iostream>

using namespace std;

int main() {
    cout << "===============================" << endl;
    cout << "       UNIVERSAL CONVERTER     " << endl;
    cout << "===============================" << endl;
    cout << "1. Celsius to Fahrenheit" << endl;
    cout << "2. Fahrenheit to Celsius" << endl;
    cout << "3. Kilometers to Miles" << endl;
    cout << "4. Miles to Kilometers" << endl;
    cout << "5. Kilograms to Pounds" << endl;
    cout << "6. Pounds to Kilograms" << endl;
    cout << "-------------------------------" << endl;
    
    int opt;
    cout << "Select conversion option (1-6): ";
    cin >> opt;
    
    double val;
    cout << "Enter value to convert: ";
    cin >> val;
    
    if (opt == 1) {
        double res = (val * 9.0 / 5.0) + 32.0;
        cout << val << " C = " << res << " F" << endl;
    } else if (opt == 2) {
        double res = (val - 32.0) * 5.0 / 9.0;
        cout << val << " F = " << res << " C" << endl;
    } else if (opt == 3) {
        double res = val * 0.621371;
        cout << val << " km = " << res << " miles" << endl;
    } else if (opt == 4) {
        double res = val * 1.60934;
        cout << val << " miles = " << res << " km" << endl;
    } else if (opt == 5) {
        double res = val * 2.20462;
        cout << val << " kg = " << res << " lbs" << endl;
    } else if (opt == 6) {
        double res = val * 0.453592;
        cout << val << " lbs = " << res << " kg" << endl;
    } else {
        cout << "Invalid option selected." << endl;
    }
    
    return 0;
}
""".trimIndent()
        ),

        CppTemplate(
            id = "fibonacci_primes",
            title = "Math & Prime Explorer",
            description = "Generate Fibonacci sequence and test primality with factors.",
            category = "Math",
            iconKey = "functions",
            colorHex = "#3B82F6",
            defaultDisplayMode = "TERMINAL",
            code = """
#include <iostream>

using namespace std;

int main() {
    cout << "===============================" << endl;
    cout << "     FIBONACCI & PRIME TOOL    " << endl;
    cout << "===============================" << endl;
    cout << "1. Generate Fibonacci Series" << endl;
    cout << "2. Check if Number is Prime" << endl;
    
    int mode;
    cout << "Choose mode (1 or 2): ";
    cin >> mode;
    
    if (mode == 1) {
        int n;
        cout << "How many terms (1-20)? ";
        cin >> n;
        
        long a = 0;
        long b = 1;
        cout << "Fibonacci sequence: ";
        for (int i = 0; i < n; i = i + 1) {
            cout << a << " ";
            long next = a + b;
            a = b;
            b = next;
        }
        cout << endl;
    } else if (mode == 2) {
        long num;
        cout << "Enter positive integer: ";
        cin >> num;
        
        bool isPrime = true;
        if (num <= 1) {
            isPrime = false;
        } else {
            for (long d = 2; d * d <= num; d = d + 1) {
                if (num % d == 0) {
                    isPrime = false;
                    break;
                }
            }
        }
        
        if (isPrime) {
            cout << ">>> " << num << " IS A PRIME NUMBER! <<<" << endl;
        } else {
            cout << ">>> " << num << " is NOT prime (composite). <<<" << endl;
        }
    }
    
    return 0;
}
""".trimIndent()
        )
    )
}
