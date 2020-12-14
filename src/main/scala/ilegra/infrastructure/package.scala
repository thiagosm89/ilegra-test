package ilegra

import org.apache.flink.api.common.typeinfo.TypeInformation

package object infrastructure {
  implicit lazy val typeInfoString = TypeInformation.of(classOf[String])

  implicit lazy val typeInfoTuple5 = TypeInformation.of(classOf[(String, String, String, String, String)])

  implicit lazy val typeInfoData = TypeInformation.of(classOf[Data])

  implicit lazy val typeInfoFile = TypeInformation.of(classOf[RowData])

  implicit lazy val typeInfoReport = TypeInformation.of(classOf[Report])

  implicit lazy val typeInfoRawData = TypeInformation.of(classOf[RawLine])

  type Row = Tuple4[String, String, String, String]
}
