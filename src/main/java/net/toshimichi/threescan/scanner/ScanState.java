package net.toshimichi.threescan.scanner;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ScanState {

    STATUS(1), LOGIN(2);

    private final int id;
}
