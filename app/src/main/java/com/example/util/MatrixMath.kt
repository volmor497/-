package com.example.util

object MatrixMath {

    fun createEmpty(rows: Int, cols: Int): Array<DoubleArray> {
        return Array(rows) { DoubleArray(cols) { 0.0 } }
    }

    fun add(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        if (a.size != b.size || a[0].size != b[0].size) {
            throw IllegalArgumentException("Dimensions do not match for addition")
        }
        val rows = a.size
        val cols = a[0].size
        val result = createEmpty(rows, cols)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result[i][j] = a[i][j] + b[i][j]
            }
        }
        return result
    }

    fun subtract(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        if (a.size != b.size || a[0].size != b[0].size) {
            throw IllegalArgumentException("Dimensions do not match for subtraction")
        }
        val rows = a.size
        val cols = a[0].size
        val result = createEmpty(rows, cols)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result[i][j] = a[i][j] - b[i][j]
            }
        }
        return result
    }

    fun multiply(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        if (a[0].size != b.size) {
            throw IllegalArgumentException("Matrix A columns must equal Matrix B rows for multiplication")
        }
        val rows = a.size
        val cols = b[0].size
        val common = b.size
        val result = createEmpty(rows, cols)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                var sum = 0.0
                for (k in 0 until common) {
                    sum += a[i][k] * b[k][j]
                }
                result[i][j] = sum
            }
        }
        return result
    }

    fun determinant(matrix: Array<DoubleArray>): Double {
        val n = matrix.size
        if (n != matrix[0].size) {
            throw IllegalArgumentException("Matrix must be square to calculate determinant")
        }
        if (n == 1) return matrix[0][0]
        if (n == 2) return matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0]
        
        var det = 0.0
        for (j in 0 until n) {
            val subMatrix = getSubmatrix(matrix, 0, j)
            val term = matrix[0][j] * determinant(subMatrix)
            if (j % 2 == 0) {
                det += term
            } else {
                det -= term
            }
        }
        return det
    }

    private fun getSubmatrix(matrix: Array<DoubleArray>, excludingRow: Int, excludingCol: Int): Array<DoubleArray> {
        val n = matrix.size
        val sub = createEmpty(n - 1, n - 1)
        var targetRow = 0
        for (i in 0 until n) {
            if (i == excludingRow) continue
            var targetCol = 0
            for (j in 0 until n) {
                if (j == excludingCol) continue
                sub[targetRow][targetCol] = matrix[i][j]
                targetCol++
            }
            targetRow++
        }
        return sub
    }
}
