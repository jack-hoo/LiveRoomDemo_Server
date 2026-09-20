package com.liveroom.service;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NameGeneratorTest {

    @Test
    public void generatesNonEmptyNameWithoutIndexOverflow() {
        // 下标算错（比如取整方式不对）会直接抛 ArrayIndexOutOfBoundsException，
        // 跑够次数就能把它逼出来。
        for (int i = 0; i < 20000; i++) {
            String name = NameGenerator.generate();
            assertFalse("昵称不应为空", name == null || name.isEmpty());
        }
    }

    @Test
    public void generatesReasonablyDiverseNames() {
        Set<String> names = new HashSet<String>();
        for (int i = 0; i < 500; i++) {
            names.add(NameGenerator.generate());
        }
        assertTrue("500 次生成应产生足够多的不同昵称，实际只有 " + names.size(), names.size() > 100);
    }
}
