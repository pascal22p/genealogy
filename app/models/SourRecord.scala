package models

import java.time.Instant

final case class SourRecord(
    id: Int,
    auth: String,
    title: String,
    abbr: String,
    publ: String,
    agnc: String,
    rin: String,
    repoId: Option[Int],
    repoCaln: String,
    repoMedi: String,
    timestamp: Instant
)
