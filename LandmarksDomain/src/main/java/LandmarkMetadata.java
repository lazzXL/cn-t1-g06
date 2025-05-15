/**
 * This class represents metadata for a landmark.
 * <p>
 * A landmark is a point of interest, which can be
 * a monument, a building, or any other significant location.
 *
 * @param name The name of the landmark.
 * @param location The location of the landmark.
 * @param confidence The confidence level of the landmark detection.
 * @param blobId The unique identifier for the landmark blob.
 */
public record LandmarkMetadata(
    String name,
    Location location,
    double confidence,
    String blobId
) {}