# Similarity of text using trigram matching in Spring with PostgreSQL

## Understanding Trigram (or Trigraph) Concepts

A trigram is a group of three consecutive characters taken from a string. We can measure the similarity of two strings by counting the number of trigrams they share. This simple idea turns out to be very effective for measuring the similarity of words in many natural languages.

---
## Creating  index

The pg_trgm module includes GiST and GIN index operator classes, which let you to establish an index over a text column for extremely quick similarity searches. These index types allow trigram-based searches for LIKE, ILIKE, ~, ~*, and = queries, in addition to the similarity operators mentioned above. In the default build of pg_trgm, similarity comparisons are case insensitive. Inequality operators aren't supported. It should be noted that such indexes may be less efficient than ordinary B-tree indexes for the equality operator.

```sql
CREATE INDEX trgm_addresses ON addresses USING gin (street gin_trgm_ops,
                                                    city gin_trgm_ops,
                                                    note gin_trgm_ops);
```
---
## Examples of queries

Example of a query that searches for addresses with a similarity of  the word 'word':

```sql
select
    a.address_id,
    a.building_no,
    a.city,
    a.house_no,
    a.note,
    a.postal_code,
    a.street
from
    addresses a
where
    a.note % 'opposite goldan'
```
result:

| address_id | building_no | city | house_no | note | postal_code | street |
| --- | --- | --- | --- | --- | --- | --- |
| e79f5e8f-0d1d-4f2a-889d-c6a567ea7b6d | "" | San Francisco | 555 | Opposite Golden Gate Park | 94101 | 555 Maple Avenue |

Result is a record with a note 'Opposite Golden Gate Park' which is similar to 'opposite goldan'. But it depends on the default threshold set in the PostgreSql. If the text is long, often a one/two word query is not enough.

Let's check it out

```sql
UPDATE addresses 
SET note = CONCAT(note, 'Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industrys standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.') 
WHERE address_id = 'e79f5e8f-0d1d-4f2a-889d-c6a567ea7b6d';
```

and run the same query again:

result:

| address_id | building_no | city | house_no | note | postal_code | street |
| --- | --- | --- | --- | --- | --- | --- |

Zero records. The default threshold is set to 0.3. If the text is long, the threshold should be set to a lower value.

However, this can be workaround by using the `similarity` function in order by clause:

```sql
select
    a.address_id,
    a.building_no,
    a.city,
    a.house_no,
    a.note,
    a.postal_code,
    a.street
from
    addresses a
order by
    SIMILARITY(a.note, 'opposite goldan') desc
```
result:

| address_id | building_no | city | house_no | note                                                | postal_code | street |
| --- | --- | --- | --- |-----------------------------------------------------| --- | --- |
| e79f5e8f-0d1d-4f2a-889d-c6a567ea7b6d | "" | San Francisco | 555 | Opposite Golden Gate Park Lorem Ipsum is simply ... | 94101 | 555 Maple Avenue |
| 11e3b1ca-38c8-4c61-b1fe-333676016f5a | "" | New York | 123 | Near Central Park                                   | 10001 | 123 Main Street |
| b3f3521e-73e6-4e94-9014-1a35b60d5a2d | A | Los Angeles | 456 | Apartment 301                                       | 90001 | 456 Elm Avenue |
| 4d8f8b3b-23e5-4ef8-ae4f-6e256bf44728 | "" | Chicago | 789 | Corner Lot                                          | 60601 | 789 Oak Boulevard |
| bb97af11-f28d-4e67-aa30-7db80f0a8790 | B | Houston | 321 | ""                                                  | 77001 | 321 Pine Street |

Now the result is sorted by similarity. The first record is the one with the highest similarity. The similarity function returns a value between 0 and 1. The higher the value, the more similar the text is.
We can apply offset and limit to the result to retrieve a specific number of records.

Implement this functionality in Spring with spring data and Criteria API.


## Register function to hibernate

For convenience, we register the function in Hibernate and will then be able to refer to it when creating a query.

Implement `PostgreSQLDialect`

```java
public class CustomPostgreSQLDialect extends PostgreSQLDialect {
  public CustomPostgreSQLDialect() {
    super();
  }

  @Override
  public void initializeFunctionRegistry(FunctionContributions functionContributions) {
    super.initializeFunctionRegistry(functionContributions);
    var functionRegistry = functionContributions.getFunctionRegistry();
    functionRegistry.registerPattern(
        "similarity",
        "SIMILARITY(?1,?2)"
    );
  }

}
```

and register this class in ‘application.yml’:

```yaml
spring:
  jpa:
    database-platform: <your-package>.CustomPostgreSQLDialect
```

## Creating query

CriteriaQuery is used to make an order by section with a specific function.
The AddressRepo class contains a findAll method that uses TextSearchableSpecification to create a predicate for the query.

```java
@Repository
@RequiredArgsConstructor
class AddressRepo implements AddressRepository {

  private final JpaAddressRepository jpaAddressRepository;

  public Slice<Address> findAll(String query, Pageable pageable) {
    return jpaAddressRepository.findAll(new TextSearchableSpecification(query), pageable);
  }

  private record TextSearchableSpecification(String query) implements Specification<Address> {
    @Override
    public Predicate toPredicate(Root<Address> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
      if (this.query != null && !this.query.isEmpty()) {
        query.orderBy(SimilarityMatchUtil.similarityOrder(cb, this.query, root.get(Address_.street), root.get(Address_.city), root.get(Address_.note)));
      }
      return null;
    }
  }

}
```
```java
public class SimilarityMatchUtil {

  private SimilarityMatchUtil() {
  }

  public static Order similarityOrder(CriteriaBuilder cb, String queryParam, Path<String>... objectPaths) {
    return Arrays.stream(objectPaths).map(e -> cb.function("similarity", Float.class, e, cb.literal(queryParam)))
        .reduce(cb::sum)
        .map(cb::desc)
        .orElse(null);
  }

}
```


Generated sql query:
```sql
select
    a1_0.address_id,
    a1_0.building_no,
    a1_0.city,
    a1_0.house_no,
    a1_0.note,
    a1_0.postal_code,
    a1_0.street
from
    addresses a1_0
order by
    ((SIMILARITY(a1_0.street, 'opposite goldan')+SIMILARITY(a1_0.city, 'opposite goldan'))+SIMILARITY(a1_0.note, 'opposite goldan')) desc
offset ? rows fetch first ? rows only
```

## Run project:

. Change the database connection settings in the `application.yml` file.
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/postgres
    username: postgres
    password: postgres
```
2. Run the project
3. Open the browser and go to [swagger](http://localhost:8080/swagger-ui/index.html)
4. Use the `GET` method to search for addresses by query. (/api/addresses)
---

*Reference:*

https://www.postgresql.org/docs/current/pgtrgm.html