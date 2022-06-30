package com.max480.randomstuff.gae.discord;

import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;

public class SodiumInstance {
    public static final LazySodiumJava sodium = new LazySodiumJava(new SodiumJava());
}
