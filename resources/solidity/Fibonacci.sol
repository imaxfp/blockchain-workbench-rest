pragma solidity ^0.4.2;


contract Fibonacci {

    event Notify(uint input, uint result);

    function fibonacci(uint number) constant returns (uint result) {
        if (number == 0) return 0;
        else if (number == 1) return 1;
        else return Fibonacci.fibonacci(number - 1) + Fibonacci.fibonacci(number - 2);
    }

    function fibonacciNotify(uint number) returns (uint result) {
        result = fibonacci(number);
        Notify(number, result);
    }

    function sum(int number1, int number2) returns(int) {
        return (number1 + number2);
    }

    function doubleNumber(uint number) constant returns (uint result) { return 2*number; }
}