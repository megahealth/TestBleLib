package io.zjw.testblelib;

/**
 * Created by Lincoln on 2018/2/25.
 */

public class ScannedDevice {
    private String name;
    private String address;
    private int rssi;

    public ScannedDevice(String name, String address, int rssi) {
        this.name = name;
        this.address = address;
        this.rssi = rssi;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    @Override
    public String toString() {
        return "ScannedDevice{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", rssi=" + rssi +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        ScannedDevice o = (ScannedDevice) obj;
        return name.equals(o.name) && address.equals(o.address);
    }
}
