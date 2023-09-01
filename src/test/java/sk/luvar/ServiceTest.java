package sk.luvar;

import net.jqwik.api.Example;

class ServiceTest {

    @Example
    void add() {
        Service service = new Service();
        service.add("Skúška");
        service.printAll();
        service.deleteAll();
        service.shutdown();
    }
}