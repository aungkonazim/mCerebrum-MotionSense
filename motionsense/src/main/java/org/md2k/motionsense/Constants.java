package org.md2k.motionsense;

import java.util.UUID;

/*
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class Constants {
    public static final String FILENAME_ASSET_METADATA = "metadata.json";
    public static final String FILENAME_INTERNAL_SDCARD_DEFAULT_CONFIG = "/mCerebrum/org.md2k.motionsense/default_config.json";
    public static final String FILENAME_INTERNAL_SDCARD_CONFIG = "/mCerebrum/org.md2k.motionsense/config.json";

    public static final String SERVICE_NAME = "org.md2k.motionsense.ServiceMotionSense";
    public static final UUID DEVICE_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    public static final UUID IMU_SERVICE_UUID = UUID.fromString("da395d22-1d81-48e2-9c68-d0ae4bbd351f");
    public static final UUID BATTERY_SERVICE_UUID = UUID.fromString("da39adf0-1d81-48e2-9c68-d0ae4bbd351f");
    public static final UUID BATTERY_SERV_CHAR_UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");
    public static final UUID IMU_SERV_CHAR_UUID = UUID.fromString("da39c921-1d81-48e2-9c68-d0ae4bbd351f");

    public static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final String MOTION_SENSE_HRV = "MotionSenseHRV";
    public static final String MOTION_SENSE = "EETech_Motion";
}
