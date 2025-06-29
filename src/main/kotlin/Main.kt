package com.example.gameeditor

import java.io.File
import java.io.IOException
import java.nio.file.InvalidPathException
import java.nio.file.Paths
import java.util.Scanner

fun main() {
    val scanner = Scanner(System.`in`)

    while (true) {
        println("\n--- 게임 에디터 기능 선택 ---")
        println("1. 디렉토리 내 공통 수정 부분 분석")
        println("2. 파일 오프셋 값 조회")
        println("3. 파일 오프셋 값 수정 및 저장 (반복 입력 후 한 번에 적용)")
        println("0. 종료")
        print("선택: ")

        when (scanner.nextLine().trim()) {
            "1" -> analyzeDirectoryCommonModifiedAreas(scanner)
            "2" -> viewFileOffsetValue(scanner)
            "3" -> modifyFileOffsetValue(scanner)
            "0" -> {
                println("프로그램을 종료합니다.")
                scanner.close()
                return
            }
            else -> println("유효하지 않은 선택입니다. 다시 입력해주세요.")
        }
    }
}

/**
 * 1. 디렉토리 내 공통 수정 부분 분석 기능
 */
fun analyzeDirectoryCommonModifiedAreas(scanner: Scanner) {
    var targetDirectory: File? = null
    println("\n--- 1. 디렉토리 내 공통 수정 부분 분석 ---")
    println("분석할 디렉토리 경로를 입력하세요.")

    while (targetDirectory == null) {
        print("디렉토리 경로 입력: ")
        val input = scanner.nextLine().trim()
        try {
            val path = Paths.get(input)
            val file = path.toFile()
            if (file.exists() && file.isDirectory) {
                targetDirectory = file
            } else {
                println("오류: '${input}' 디렉토리가 존재하지 않거나 유효하지 않습니다.")
            }
        } catch (e: InvalidPathException) {
            println("오류: 유효하지 않은 디렉토리 경로입니다. (${e.message})")
        }
    }

    println("\n--- 디렉토리 내 파일 목록 수집 ---")
    val allFilesInDirectory = targetDirectory.walkTopDown()
        .filter { it.isFile && !it.name.startsWith(".") }
        .toList()

    if (allFilesInDirectory.isEmpty()) {
        println("디렉토리 내에 비교할 파일이 없습니다 (숨김 파일 제외).")
    } else {
        println("총 ${allFilesInDirectory.size}개의 파일을 찾았습니다 (숨김 파일 제외).")
        val fileNames = allFilesInDirectory.map { it.name }
        println("\n--- 모든 파일 간 공통 수정 영역 분석 시작 ---")
        val modifiedAreas = FileOperations.findCommonModifiedAreas(allFilesInDirectory)
        FileOperations.printCommonModifiedAreas(modifiedAreas, fileNames)
    }
}

/**
 * 2. 파일 오프셋 값 조회 기능
 */
fun viewFileOffsetValue(scanner: Scanner) {
    println("\n--- 2. 파일 오프셋 값 조회 ---")
    print("조회할 파일 경로 입력: ")
    val filePath = scanner.nextLine().trim()
    val file = File(filePath)

    if (!file.exists() || !file.isFile) {
        println("오류: '${filePath}' 파일이 존재하지 않거나 유효하지 않습니다.")
        return
    }

    print("조회할 **오프셋(10진수)** 입력 (0부터 시작): ")
    val offsetInput = scanner.nextLine().trim()
    val offset = offsetInput.toLongOrNull()

    if (offset == null || offset < 0) {
        println("오류: 유효하지 않은 오프셋 값입니다. 양의 정수를 입력하세요.")
        return
    }

    val byteValue = FileOperations.getByteAtOffset(file, offset)
    if (byteValue != null) {
        println("파일 '${file.name}'의 오프셋 ${offset} (10진수) 값: 0x${String.format("%02X", byteValue.toInt() and 0xFF)}")
    }
}

/**
 * 3. 파일 오프셋 값 수정 및 저장 기능 (반복 입력 후 한 번에 적용)
 */
fun modifyFileOffsetValue(scanner: Scanner) {
    println("\n--- 3. 파일 오프셋 값 수정 및 저장 ---")
    print("수정할 원본 파일 경로 입력: ")
    val filePath = scanner.nextLine().trim()
    val originalFile = File(filePath)

    if (!originalFile.exists() || !originalFile.isFile) {
        println("오류: '${filePath}' 파일이 존재하지 않거나 유효하지 않습니다.")
        return
    }

    // 파일을 메모리에 로드
    val fileBytes: ByteArray
    try {
        fileBytes = originalFile.readBytes() // 파일 전체를 ByteArray로 읽어옵니다.
        println("파일 '${originalFile.name}'을 메모리에 로드했습니다. (크기: ${fileBytes.size} 바이트)")
    } catch (e: IOException) {
        println("오류: 파일 '${originalFile.name}'을 읽을 수 없습니다. ${e.message}")
        return
    } catch (e: OutOfMemoryError) {
        println("오류: 파일이 너무 커서 메모리에 로드할 수 없습니다. 더 작은 파일을 시도하거나 다른 방식을 고려해주세요.")
        return
    }


    println("\n**오프셋(10진수)**과 **새로운 바이트 값(16진수)**을 공백으로 구분하여 입력하세요.")
    println("예: 10 FF (10진수 오프셋 10, 값 FF)")
    println("모든 수정을 마쳤으면 'done'을 입력하여 저장합니다.")

    var hasModifications = false // 변경 사항이 있었는지 추적

    while (true) {
        print("입력 (오프셋 값 또는 'done'): ")
        val input = scanner.nextLine().trim()

        if (input.equals("done", ignoreCase = true)) {
            break // done 입력 시 루프 종료 후 저장
        }

        val parts = input.split(" ")
        if (parts.size != 2) {
            println("오류: '오프셋 값' 형식으로 입력해주세요. 예: 10 FF")
            continue
        }

        val offsetInput = parts[0]
        val newValueString = parts[1]

        val offset = offsetInput.toLongOrNull()
        if (offset == null || offset < 0) {
            println("오류: 유효하지 않은 오프셋 값입니다. 양의 정수를 10진수로 입력하세요.")
            continue
        }

        val newValue = newValueString.toIntOrNull(16) // 16진수 문자열을 Int로 파싱
        if (newValue == null || newValue !in 0..255) {
            println("오류: 유효하지 않은 바이트 값입니다. 00부터 FF 사이의 16진수 값을 입력하세요.")
            continue
        }

        // 메모리 상의 ByteArray에 변경 반영
        if (offset < fileBytes.size) {
            val oldByte = fileBytes[offset.toInt()]
            fileBytes[offset.toInt()] = newValue.toByte()
            println("메모리 상에서 오프셋 ${offset} (10진수)의 값을 0x${String.format("%02X", oldByte.toInt() and 0xFF)} -> 0x${String.format("%02X", newValue)}로 변경했습니다.")
            hasModifications = true
        } else {
            // 오프셋이 현재 파일 크기를 넘어설 경우 (파일 확장)
            println("경고: 오프셋 ${offset} (10진수)이 현재 파일 크기 ${fileBytes.size}를 초과합니다.")
            println("      이 오프셋은 현재 파일에 존재하지 않으므로 변경을 반영할 수 없습니다.")
            println("      현재 구현 방식에서는 파일 크기 확장 기능을 지원하지 않습니다.")
            // TODO: 필요시, 여기서 ByteArray의 크기를 늘리는 로직을 추가해야 함
            // (예: copyOf 또는 System.arraycopy를 사용하여 새 ByteArray 생성 및 값 복사)
            // 현재는 파일 크기 확장을 지원하지 않으므로 경고만 출력합니다.
        }
    }

    // 'done' 입력 시 변경된 내용 한 번에 저장
    if (hasModifications) {
        val newFileName = originalFile.name
        val newFile = File(originalFile.parentFile, newFileName)
        try {
            newFile.writeBytes(fileBytes) // 메모리의 ByteArray를 파일에 한 번에 씁니다.
            println("성공: 모든 변경 사항을 '${newFile.name}' 파일에 저장했습니다.")
        } catch (e: IOException) {
            println("오류: '${newFile.name}' 파일을 저장할 수 없습니다. ${e.message}")
        }
    } else {
        println("메모리에서 변경된 내용이 없어 새로운 파일을 저장하지 않습니다.")
    }
}