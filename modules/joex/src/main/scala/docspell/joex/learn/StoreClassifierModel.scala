/*
 * Copyright 2020 Eike K. & Contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package docspell.joex.learn

import cats.effect._
import cats.implicits._
import fs2.io.file.Files

import docspell.analysis.classifier.ClassifierModel
import docspell.common._
import docspell.joex.scheduler._
import docspell.store.Store
import docspell.store.records.RClassifierModel

object StoreClassifierModel {

  def handleModel[F[_]: Async](
      ctx: Context[F, _],
      collective: Ident,
      modelName: ClassifierName
  )(
      trainedModel: ClassifierModel
  ): F[Unit] =
    handleModel(ctx.store, ctx.logger)(collective, modelName, trainedModel)

  def handleModel[F[_]: Async](
      store: Store[F],
      logger: Logger[F]
  )(
      collective: Ident,
      modelName: ClassifierName,
      trainedModel: ClassifierModel
  ): F[Unit] =
    for {
      oldFile <- store.transact(
        RClassifierModel.findByName(collective, modelName.name).map(_.map(_.fileId))
      )
      _ <- logger.debug(s"Storing new trained model for: ${modelName.name}")
      fileData = Files[F].readAll(trainedModel.model)
      newFileId <-
        fileData.through(store.fileStore.save(MimeTypeHint.none)).compile.lastOrError
      _ <- store.transact(
        RClassifierModel.updateFile(collective, modelName.name, newFileId)
      )
      _ <- logger.debug(s"New model stored at file ${newFileId.id}")
      _ <- oldFile match {
        case Some(fid) =>
          logger.debug(s"Deleting old model file ${fid.id}") *>
            store.fileStore.delete(fid)
        case None => ().pure[F]
      }
    } yield ()
}
