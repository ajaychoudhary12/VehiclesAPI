package com.udacity.vehicles.service;

import com.udacity.vehicles.client.maps.MapsClient;
import com.udacity.vehicles.client.prices.Price;
import com.udacity.vehicles.client.prices.PriceClient;
import com.udacity.vehicles.domain.Location;
import com.udacity.vehicles.domain.car.Car;
import com.udacity.vehicles.domain.car.CarRepository;
import java.util.List;

import com.udacity.vehicles.domain.manufacturer.Manufacturer;
import com.udacity.vehicles.domain.manufacturer.ManufacturerRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Implements the car service create, read, update or delete
 * information about vehicles, as well as gather related
 * location and price data when desired.
 */
@Service
public class CarService {

    private final CarRepository repository;
    private final MapsClient mapsWebClient;
    private final PriceClient pricingWebClient;

    private final ManufacturerRepository manufacturerRepository;

    public CarService(CarRepository repository, MapsClient mapsWebClient, PriceClient pricingWebClient, ManufacturerRepository manufacturerRepository) {
        this.mapsWebClient = mapsWebClient;
        this.pricingWebClient = pricingWebClient;
        this.repository = repository;
        this.manufacturerRepository = manufacturerRepository;
    }

    /**
     * Gathers a list of all vehicles
     * @return a list of all vehicles in the CarRepository
     */
    public List<Car> list() {
        return repository.findAll();
    }

    /**
     * Gets car information by ID (or throws exception if non-existent)
     * @param id the ID number of the car to gather information on
     * @return the requested car's information, including location and price
     */
    public Car findById(Long id) {
        Car car = repository.findById(id)
                .orElseThrow(CarNotFoundException::new);

        String price = pricingWebClient.getPrice(id);
        car.setPrice(price);

        Location location = mapsWebClient.getAddress(car.getLocation());
        car.setLocation(location);

        return car;
    }

    /**
     * Either creates or updates a vehicle, based on prior existence of car
     * @param car A car object, which can be either new or existing
     * @return the new/updated car is stored in the repository
     */
    public Car save(Car car) {

        Manufacturer manufacturer = car.getDetails().getManufacturer();
        if (manufacturer != null && manufacturer.getCode() == null) {
            manufacturer = manufacturerRepository.save(manufacturer);
            car.getDetails().setManufacturer(manufacturer);
        }

        if (car.getId() != null) {
            return repository.findById(car.getId())
                    .map(carToBeUpdated -> {
                        carToBeUpdated.setCondition(car.getCondition());
                        carToBeUpdated.setDetails(car.getDetails());
                        carToBeUpdated.setLocation(car.getLocation());
                        return repository.save(carToBeUpdated);
                    }).orElseThrow(CarNotFoundException::new);
        }

        return repository.save(car);
    }

    /**
     * Deletes a given car by ID
     * @param id the ID number of the car to delete
     */
    public void delete(Long id) {
        Car car = repository.findById(id)
                .orElseThrow(CarNotFoundException::new);
        repository.delete(car);
    }
}
