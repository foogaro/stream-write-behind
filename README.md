# Write-Behind-Streaming

This library provides a streamlined implementation of the write-behind caching pattern, enabling efficient and consistent data management for applications with high-performance needs.

## What is the write-behind caching pattern?

The write-behind caching pattern allows applications to update data directly in the cache and later propagate these updates to the underlying database. This approach keeps the data in the database consistent with the latest cached data, without delaying application performance due to direct database writes.

## Why write to the cache?

Writing to the cache is significantly faster than writing to the database, improving application response times. Additionally, since reads are performed directly from the cache, data access is immediate, and your read operations remain consistent with the cached data.


## How does it work?

This library uses Redis Streams as a communication channel to connect the cache and underlying data storage seamlessly. When an entity is updated in the cache, an event or message is generated in Redis Streams. This message contains the entity’s data and is picked up by the respective repository, which then persists the update to the target data platform, such as SQL Server, PostgreSQL, or any other supported database.

Event consumption is managed by a built-in consumer, provided out-of-the-box by the library. For each repository, there is a dedicated consumer, allowing for parallel processing across multiple repositories. This design enables updates to be written to Redis, SQL Server, PostgreSQL, and other databases in near real-time, ensuring that data remains synchronized across all platforms.

This architecture allows for smooth and efficient data flow, where data is first cached for fast access, then reliably propagated to underlying databases with minimal delay, supporting consistency and high performance across your application’s data management layers.

The following high-level architecture illustrates how the flow works:

<p align="center"><img src="images/hla.png" alt="Write Behind" width="600"/></p>

## What happens if something goes wrong?

Redis Streams includes a Pending Entry List, which keeps track of all messages that have been processed but not yet acknowledged by consumers. The library periodically checks this list for any pending messages and reattempts to process them until they are either successfully acknowledged or reach the maximum number of allowed processing attempts. Additionally, if a message has been pending beyond a specified time threshold (acting as a timeout), it is flagged for special handling.

When either the maximum attempts are reached or the timeout occurs, the message is moved to a Dead Letter Queue (DLQ) stream. This DLQ serves as a holding area for messages requiring further attention, allowing for debugging and potential reprocessing. By using the DLQ, the library ensures that no data is lost, providing a safety net for handling unprocessed messages in a controlled and recoverable manner.

## How to Use It

The write-behind-streaming library is built to be straightforward and user-friendly. To implement the write-behind caching pattern, simply add the `@WriteBehind` annotation to any entities you want to include, as shown below:

By marking entities with `@WriteBehind`, the library automatically detects and manages them, generating the required logic to handle data updates in the cache and ensuring they are later written to the database. This setup reduces the complexity of implementing write-behind caching, allowing developers to achieve fast, consistent data updates with minimal configuration.

```java
@Entity
@Table(name = "employers")
@WriteBehind
public class Employer {

    @Id
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "phone", nullable = true)
    private String phone;

    public Employer() {
    }

    public Employer(String name, String address, String email, String phone) {
        this.name = name;
        this.address = address;
        this.email = email;
        this.phone = phone;
    }

    // Getters and Setters

}
```

Once entities are annotated with `@WriteBehind`, the library automatically identifies them and generates the necessary logic to manage write-behind caching. This means that whenever data is updated in the cache, the library takes care of asynchronously propagating these changes to the underlying database, maintaining data consistency across both layers.

With this setup, you gain the advantages of faster write operations directly to the cache, while the library seamlessly handles the synchronization of data to the database in the background. This approach ensures efficient, high-speed data access and consistency with minimal configuration, allowing developers to focus on application logic without worrying about complex caching management.
