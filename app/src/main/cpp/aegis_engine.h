#ifndef AEGIS_FORENSIC_ENGINE_H
#define AEGIS_FORENSIC_ENGINE_H

#include <cstdint>
#include <cstddef>
#include <vector>
#include <string>

namespace aegis {

struct SignaturePattern {
    std::string name;
    std::string extension;
    std::vector<uint8_t> header;
    std::vector<uint8_t> footer;
    size_t max_size;
    bool has_footer;
};

struct CarveHit {
    uint64_t start_offset;
    uint64_t end_offset;
    uint64_t length;
    std::string file_type;
    std::string extension;
    double confidence;
    uint32_t crc32;
    double entropy;
    bool complete_boundary;
};

struct FragmentBlock {
    uint64_t offset;
    size_t length;
    double entropy;
    uint32_t crc32;
    bool matches_prev_stride;
};

// Fast vector/byte scanning
int64_t find_pattern(const uint8_t* buffer, size_t buffer_len, const uint8_t* pattern, size_t pattern_len);

// Fast Shannon Entropy computation (0.0 to 8.0)
double calculate_entropy(const uint8_t* data, size_t len);

// Fast CRC32
uint32_t calculate_crc32(const uint8_t* data, size_t len);

// Frequency distribution and printable string density
void analyze_byte_distribution(const uint8_t* data, size_t len,
                               double* printable_ratio,
                               double* null_ratio,
                               double* high_entropy_ratio);

// .BIN Stride and repeated structure detector
size_t detect_record_stride(const uint8_t* data, size_t len, size_t min_stride, size_t max_stride);

// Fragment continuity scoring between block A and block B
double evaluate_fragment_continuity(const uint8_t* block_a, size_t len_a,
                                    const uint8_t* block_b, size_t len_b);

} // namespace aegis

#endif // AEGIS_FORENSIC_ENGINE_H
