#ifndef BULLETPROOFS_GEO_H
#define BULLETPROOFS_GEO_H

#include <cstdarg>
#include <cstdint>
#include <cstdlib>
#include <ostream>
#include <new>

namespace bulletproofs_geo {

constexpr static const double BPG_RADIUS_AT_EQUATOR = 6378137.0;

constexpr static const double BPG_FLATTENING_ELIPSOID = (1.0 / 298.257223563);

constexpr static const double BPG_RADIUS_AT_POLES = ((1.0 - BPG_FLATTENING_ELIPSOID) * BPG_RADIUS_AT_EQUATOR);

constexpr static const uint32_t BPG_MAX_ITERATIONS = 200;

constexpr static const double BPG_CONVERGENCE_THRESHOLD = 0.000000000001;

constexpr static const int32_t BPG_PRECISION = 6;

struct BPG_ProofData;

} // namespace bulletproofs_geo

#endif // BULLETPROOFS_GEO_H
