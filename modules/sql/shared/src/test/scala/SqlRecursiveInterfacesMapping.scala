// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.grackle.sql.test

import cats.kernel.Eq
import io.circe.Encoder

import edu.gemini.grackle._
import syntax._
import Predicate._

trait SqlRecursiveInterfacesMapping[F[_]] extends SqlTestMapping[F] { self =>
  def itemType: Codec

  object items extends TableDef("recursive_interface_items") {
    val id       = col("id", text)
    val itemType = col("item_type", self.itemType)
  }

  object nextItems extends TableDef("recursive_interface_next_items") {
    val id       = col("id", text)
    val nextItem = col("next_item", nullable(text))
  }

  val schema =
    schema"""
      type Query {
        items: [Item!]!
      }
      interface Item {
        id: ID!
        itemType: ItemType!
      }
      type ItemA implements Item {
        id: ID!
        itemType: ItemType!
        nextItem: Item
      }
      type ItemB implements Item {
        id: ID!
        itemType: ItemType!
        nextItem: Item
      }
      enum ItemType {
        ITEM_A
        ITEM_B
      }
    """

  val QueryType = schema.ref("Query")
  val IType = schema.ref("Item")
  val ItemTypeType = schema.ref("ItemType")
  val ItemAType = schema.ref("ItemA")
  val ItemBType = schema.ref("ItemB")

  val typeMappings =
    List(
      ObjectMapping(
        tpe = QueryType,
        fieldMappings =
          List(
            SqlObject("items")
          )
      ),
      SqlInterfaceMapping(
        tpe = IType,
        discriminator = itemTypeDiscriminator,
        fieldMappings =
          List(
            SqlField("id", items.id, key = true),
            SqlField("itemType", items.itemType, discriminator = true),
          )
      ),
      ObjectMapping(
        tpe = ItemAType,
        fieldMappings =
          List(
            SqlField("id", items.id, key = true),
            SqlObject("nextItem", Join(items.id, nextItems.id), Join(nextItems.nextItem, items.id))
          )
      ),
      ObjectMapping(
        tpe = ItemBType,
        fieldMappings =
          List(
            SqlField("id", items.id, key = true),
            SqlObject("nextItem", Join(items.id, nextItems.id), Join(nextItems.nextItem, items.id))
          )
      ),
      LeafMapping[ItemType](ItemTypeType)
    )

  object itemTypeDiscriminator extends SqlDiscriminator {
    def discriminate(c: Cursor): Result[Type] = {
      for {
        et <- c.fieldAs[ItemType]("itemType")
      } yield et match {
        case ItemType.ItemA => ItemAType
        case ItemType.ItemB => ItemBType
      }
    }

    def narrowPredicate(subtpe: Type): Option[Predicate] = {
      def mkPredicate(tpe: ItemType): Option[Predicate] =
        Some(Eql(IType / "itemType", Const(tpe)))

      subtpe match {
        case ItemAType => mkPredicate(ItemType.ItemA)
        case ItemBType => mkPredicate(ItemType.ItemB)
        case _ => None
      }
    }
  }

  sealed trait ItemType extends Product with Serializable
  object ItemType {
    case object ItemA extends ItemType
    case object ItemB extends ItemType

    implicit val itemTypeEq: Eq[ItemType] = Eq.fromUniversalEquals[ItemType]

    def fromString(s: String): Option[ItemType] =
      s.trim.toUpperCase match {
        case "ITEM_A"  => Some(ItemA)
        case "ITEM_B" => Some(ItemB)
        case _ => None
      }

    implicit val itemTypeEncoder: io.circe.Encoder[ItemType] =
      Encoder[String].contramap(_ match {
        case ItemA => "FILM"
        case ItemB => "SERIES"
      })

    def fromInt(i: Int): ItemType =
      (i: @unchecked) match {
        case 1 => ItemA
        case 2 => ItemB
      }

    def toInt(i: ItemType): Int =
      i match {
        case ItemA  => 1
        case ItemB => 2
      }
  }
}
