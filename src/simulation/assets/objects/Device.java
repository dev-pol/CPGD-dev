package simulation.assets.objects;

import simulation.assets.Asset;

public class Device extends Asset {

    public Device() {
        this.setName("DummyDevice");
    }

    public Device(double lat, double lon, double height) {
        this.setName("DummyDevice");
        this.setLat(lat);
        this.setLon(lon);
        this.setHeight(height);
    }

    public Device(int id, double lat, double lon, double height) {
        this.setId(id);
        this.setLat(lat);
        this.setLon(lon);
        this.setHeight(height);
    }

    public Device(int id, String name, double lat, double lon, double height) {
        this.setId(id);
        this.setName(name);
        this.setLat(lat);
        this.setLon(lon);
        this.setHeight(height);
    }

}
