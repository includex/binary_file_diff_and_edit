package com.example.gameeditor

import java.io.File
import java.io.FileInputStream
// import java.io.FileOutputStream // 사용되지 않으므로 제거

object FileOperations {

    /**
     * 여러 바이너리 파일 간의 공통적으로 수정된 영역을 찾아 반환합니다.
     * "공통적으로 수정된 영역"은 주어진 모든 파일에서 해당 오프셋의 바이트 값이
     * 서로 모두 다를 때를 의미합니다. (예: 0x01, 0x02, 0x03 처럼 모든 값이 다를 때)
     *
     * @param files 비교할 바이너리 파일들의 리스트
     * @return 오프셋(바이트 위치, 0부터 시작)을 키로, 해당 오프셋에서 발견된
     * 모든 파일의 바이트 값 리스트를 값으로 하는 맵.
     */
    fun findCommonModifiedAreas(files: List<File>): Map<Long, List<Byte>> {
        if (files.isEmpty()) {
            return emptyMap()
        }

        // 모든 파일의 입력 스트림을 엽니다. 안전하게 닫히도록 use 함수를 사용합니다.
        val inputStreams = files.map { FileInputStream(it) }
        val commonModifiedBytes: MutableMap<Long, MutableList<Byte>> = mutableMapOf()

        try {
            var currentOffset: Long = 0
            var allFilesEnded = false

            while (!allFilesEnded) {
                val bytesAtOffset = mutableListOf<Byte>()
                var hasAnyByte = false

                // 모든 스트림에서 현재 오프셋의 바이트를 읽습니다.
                for (stream in inputStreams) {
                    val currentByte = if (stream.available() > 0) {
                        hasAnyByte = true
                        stream.read().toByte()
                    } else {
                        null
                    }
                    bytesAtOffset.add(currentByte ?: 0)
                }

                allFilesEnded = !hasAnyByte

                // 바이트 리스트 내의 고유한 값의 개수가 파일의 총 개수와 같으면 (즉, 모든 값이 다르면) 수정된 영역으로 간주
                if (bytesAtOffset.distinct().size == files.size) {
                    commonModifiedBytes[currentOffset] = bytesAtOffset
                }

                if (hasAnyByte) {
                    currentOffset++
                } else if (allFilesEnded) {
                    break
                }
            }
        } finally {
            inputStreams.forEach { it.close() }
        }
        return commonModifiedBytes
    }

    /**
     * 공통 수정 영역을 콘솔에 보기 좋게 출력합니다.
     * 바이트 값을 16진수 형태로 표시합니다.
     *
     * @param modifiedAreas findCommonModifiedAreas 함수에서 반환된 맵
     * @param fileNames 비교에 사용된 파일들의 이름 리스트
     */
    fun printCommonModifiedAreas(modifiedAreas: Map<Long, List<Byte>>, fileNames: List<String>) {
        if (modifiedAreas.isEmpty()) {
            println("  -> 모든 파일의 해당 오프셋 바이트 값이 모두 다른 영역이 없습니다.")
            return
        }

        println("  --- 모든 파일의 바이트 값이 서로 다른 영역 ---")
        modifiedAreas.forEach { (offset, contents) ->
            // 오프셋을 10진수로 출력
            print("    오프셋 ${offset} (10진수): ")
            contents.forEachIndexed { fileIndex, byteValue ->
                val byteString = String.format("%02X", byteValue.toInt() and 0xFF)
                print("[${fileNames.getOrNull(fileIndex) ?: "Unknown File"}]: 0x$byteString ")
            }
            println()
        }
    }

    /**
     * 특정 파일의 특정 오프셋에 있는 바이트 값을 조회합니다.
     *
     * @param file 조회할 파일
     * @param offset 조회할 바이트 오프셋 (0부터 시작)
     * @return 해당 오프셋의 바이트 값 (0-255) 또는 오프셋이 유효하지 않으면 null
     */
    fun getByteAtOffset(file: File, offset: Long): Byte? {
        if (!file.exists() || !file.isFile) {
            println("오류: '${file.absolutePath}' 파일이 존재하지 않거나 유효하지 않습니다.")
            return null
        }
        if (offset < 0) {
            println("오류: 오프셋은 음수일 수 없습니다.")
            return null
        }

        FileInputStream(file).use { fis ->
            if (offset >= fis.channel.size()) {
                println("오류: 오프셋 ${offset} (10진수)이 파일 크기 ${fis.channel.size()}를 초과합니다.")
                return null
            }
            fis.skip(offset) // 해당 오프셋까지 건너뜀
            val byteValue = fis.read()
            return if (byteValue != -1) byteValue.toByte() else null // -1은 스트림의 끝을 의미
        }
    }

    // writeByteAtOffsetAndSave 함수는 Main.kt의 modifyFileOffsetValue 함수로 로직이 통합되므로 제거됩니다.
}