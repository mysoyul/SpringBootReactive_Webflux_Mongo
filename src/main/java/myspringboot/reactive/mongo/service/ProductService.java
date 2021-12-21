package myspringboot.reactive.mongo.service;

import lombok.RequiredArgsConstructor;
import myspringboot.reactive.mongo.dto.ProductDto;
import myspringboot.reactive.mongo.entity.Product;
import myspringboot.reactive.mongo.repository.ProductRepository;
import myspringboot.reactive.mongo.utils.AppUtils;
import org.springframework.data.domain.Range;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository repository;

    public Flux<ProductDto> getAllProducts() {
        return repository.findAll() //Flux<Product>
                .map(AppUtils::entityToDto);
                //.map(product -> AppUtils.entityToDto(product));
    }

    public Mono<ProductDto> getProduct(String id) {
        return repository.findById(id)
                .map(AppUtils::entityToDto);
    }

    //ResponseEntity = body + status + header
    public Mono<ResponseEntity<ProductDto>> getProductRE(String id) {
        return repository.findById(id)
                .map(product -> ResponseEntity.ok(AppUtils.entityToDto(product)))
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));    //404
    }

    //public final <R> Mono<R> flatMap(Function<? super T, ? extends Mono<? extends R>> transformer)
    public Mono<ProductDto> saveProduct(Mono<ProductDto> productDtoMono){
        return productDtoMono.map(AppUtils::dtoToEntity)  //Mono<ProdctDto> -> Mono<Product>
                //.flatMap(product -> repository.insert(product))
                .flatMap(repository::insert)  //Mono<Product>
                .map(AppUtils::entityToDto);  //Mono<Prodct> -> Mono<ProductDto>
    }

    public Mono<ResponseEntity<ProductDto>> saveProductRE(Mono<ProductDto> productDtoMono) {
        return productDtoMono.map(AppUtils::dtoToEntity)
                .flatMap(repository::insert)
                .map(insProduct -> ResponseEntity.ok(AppUtils.entityToDto(insProduct)))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build());  //406
    }

    public Mono<ProductDto> updateProduct(Mono<ProductDto> productDtoMono, String id){
        return repository.findById(id)
                //.flatMap(existProduct -> productDtoMono.map(dto -> AppUtils.dtoToEntity(dto))) //Mono<Product>
                .flatMap(existProduct -> productDtoMono.map(AppUtils::dtoToEntity))
                .doOnNext(product -> product.setId(id))
                .flatMap(repository::save)
                .map(AppUtils::entityToDto);
    }

    public Mono<ResponseEntity<ProductDto>> updateProductRE(Mono<ProductDto> productDtoMono, String id){
        return repository.findById(id)
                .flatMap(existProduct -> productDtoMono.map(AppUtils::dtoToEntity))
                .doOnNext(product -> product.setId(id))
                .flatMap(repository::save)
                .map(updProduct -> ResponseEntity.ok(AppUtils.entityToDto(updProduct)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    public Mono<ResponseEntity<Void>> deleteProduct(String id) {
        return repository.findById(id)
                .flatMap(existProduct ->
                            repository.delete(existProduct)
                                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                        )
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    public Flux<ProductDto> getRegexByName(String name) {
        return repository.findByName(name).map(AppUtils::entityToDto);
    }

    public Flux<ProductDto> getPriceByRangge(double min, double max) {
        return repository.findByPriceBetween(Range.closed(min, max)).map(AppUtils::entityToDto);
    }
}
