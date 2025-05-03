package sollecitom.exercises.edttt

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import assertk.assertions.support.expected
import assertk.assertions.support.show
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import sollecitom.exercises.edttt.TicTacToe.Companion.possibleWinningSequences
import sollecitom.exercises.edttt.TicTacToe.Position
import sollecitom.exercises.edttt.TicTacToe.Position.Column
import sollecitom.exercises.edttt.TicTacToe.Position.Column.*
import sollecitom.exercises.edttt.TicTacToe.Position.Row
import sollecitom.exercises.edttt.TicTacToe.Position.Row.*
import sollecitom.exercises.edttt.TicTacToe.WinningSequence
import sollecitom.exercises.edttt.TicTacToe.WinningSequence.*
import sollecitom.exercises.edttt.TicTacToeMatch.Event.*
import sollecitom.exercises.edttt.TicTacToeMatch.State.*
import sollecitom.libs.swissknife.core.domain.identity.Id
import sollecitom.libs.swissknife.core.domain.identity.factory.invoke
import sollecitom.libs.swissknife.core.domain.text.Name
import sollecitom.libs.swissknife.core.domain.traits.Identifiable
import sollecitom.libs.swissknife.core.domain.traits.Timestamped
import sollecitom.libs.swissknife.core.test.utils.testProvider
import sollecitom.libs.swissknife.core.utils.CoreDataGenerator
import sollecitom.libs.swissknife.core.utils.RandomGenerator
import sollecitom.libs.swissknife.core.utils.TimeGenerator
import sollecitom.libs.swissknife.core.utils.UniqueIdGenerator
import sollecitom.libs.swissknife.test.utils.assertions.failedThrowing
import sollecitom.libs.swissknife.test.utils.execution.utils.test
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@TestInstance(PER_CLASS)
class EventDrivenTicTacToeTests : CoreDataGenerator by CoreDataGenerator.testProvider {

    // TODO switch from sequential events to historical facts with partial causality

    private val timeout = 5.seconds

    @Test
    fun `two players playing a match`() = testWithMatchAndPlayers(timeout = timeout) { player1, player2, match ->

        player1.waitForTurnAndClaimPosition(row = MID, column = CENTRE)
        player2.waitForTurnAndClaimPosition(row = BOTTOM, column = LEFT)
        player1.waitForTurnAndClaimPosition(row = TOP, column = RIGHT)
        player2.waitForTurnAndClaimPosition(row = MID, column = LEFT)
        player1.waitForTurnAndClaimPosition(row = TOP, column = LEFT)
        player2.waitForTurnAndClaimPosition(row = TOP, column = CENTRE)
        player1.waitForTurnAndClaimPosition(row = BOTTOM, column = RIGHT)

        assertThat(match).wasWon().by(player1).withSequence(LeftRightDiagonal)
    }

    @Test
    fun `rehydrating a match from its events`() = test(timeout = timeout) {

        val framework = newInMemoryEventFramework()
        val (player1, player2, matchId) = List(3) { newId() }
        val events = ticTacToeMatchEvents(matchId = matchId, firstPlayer = player1, secondPlayer = player2) {
            match.wasCreated()
            firstPlayer.claimedPosition(row = MID, column = CENTRE)
            secondPlayer.claimedPosition(row = BOTTOM, column = LEFT)
            firstPlayer.claimedPosition(row = TOP, column = RIGHT)
            secondPlayer.claimedPosition(row = MID, column = LEFT)
            firstPlayer.claimedPosition(row = TOP, column = LEFT)
            secondPlayer.claimedPosition(row = TOP, column = CENTRE)
            firstPlayer.claimedPosition(row = BOTTOM, column = RIGHT)
        }

        framework.publishTicTacToeMatchEvents(matchId, events)
        val ticTacToe = TicTacToe.newGameEngine(framework = framework)
        val match = ticTacToe.matchWithId(matchId)

        assertThat(match).wasWon().by(player1).withSequence(LeftRightDiagonal)
    }

    @Test
    fun `a match resulting in a draw`() = testWithMatchAndPlayers(timeout = timeout) { player1, player2, match ->

        player1.waitForTurnAndClaimPosition(row = MID, column = CENTRE)
        player2.waitForTurnAndClaimPosition(row = BOTTOM, column = LEFT)
        player1.waitForTurnAndClaimPosition(row = TOP, column = RIGHT)
        player2.waitForTurnAndClaimPosition(row = TOP, column = LEFT)
        player1.waitForTurnAndClaimPosition(row = MID, column = LEFT)
        player2.waitForTurnAndClaimPosition(row = MID, column = RIGHT)
        player1.waitForTurnAndClaimPosition(row = BOTTOM, column = RIGHT)
        player2.waitForTurnAndClaimPosition(row = BOTTOM, column = CENTRE)
        player1.waitForTurnAndClaimPosition(row = TOP, column = CENTRE)

        assertThat(match).wasADraw()
    }

    @Test
    fun `a player attempts to claim a position already claimed by the other player`() = testWithMatchAndPlayers { player1, player2, match ->

        player1.waitForTurnAndClaimPosition(row = MID, column = CENTRE)

        val position = Position(row = MID, column = CENTRE)
        val attempt = runCatching { player2.waitForTurnAndClaimPosition(position = position) }

        assertThat(attempt).failedThrowing(PositionAlreadyClaimedException(position = position, attemptingPlayer = player2.id, controllingPlayerId = player1.id, matchId = match.id))
    }

    @Test
    fun `a player attempts to claim a position already claimed by themselves`() = testWithMatchAndPlayers { player1, player2, match ->

        val position = Position(row = MID, column = CENTRE)
        player1.waitForTurnAndClaimPosition(position = position)
        player2.waitForTurnAndClaimPosition(row = BOTTOM, column = LEFT)

        val attempt = runCatching { player1.waitForTurnAndClaimPosition(position = position) }

        assertThat(attempt).failedThrowing(PositionAlreadyClaimedException(position = position, attemptingPlayer = player1.id, controllingPlayerId = player1.id, matchId = match.id))
    }

    @Test
    fun `a player attempts to move outside their turn`() = testWithMatchAndPlayers { player1, player2, match ->

        player1.waitForTurnAndClaimPosition(row = TOP, column = RIGHT)
        player2.waitForTurnAndClaimPosition(row = BOTTOM, column = LEFT)

        val attempt = runCatching { player2.claimPosition(row = MID, column = CENTRE) }

        assertThat(attempt).failedThrowing(IllegalTurnException(attemptingPlayer = player2.id, currentPlayer = player1.id, matchId = match.id))
    }

    @Test
    fun `a player attempts to move after the match is already won`() = testWithMatchAndPlayers { player1, player2, _ ->

        player1.waitForTurnAndClaimPosition(row = MID, column = CENTRE)
        player2.waitForTurnAndClaimPosition(row = BOTTOM, column = LEFT)
        player1.waitForTurnAndClaimPosition(row = TOP, column = RIGHT)
        player2.waitForTurnAndClaimPosition(row = MID, column = LEFT)
        player1.waitForTurnAndClaimPosition(row = TOP, column = LEFT)
        player2.waitForTurnAndClaimPosition(row = TOP, column = CENTRE)
        player1.waitForTurnAndClaimPosition(row = BOTTOM, column = RIGHT)

        val attempt = runCatching { player2.claimPosition(row = BOTTOM, column = CENTRE) }

        assertThat(attempt).failedThrowing<AlreadyTerminatedMatchException>()
    }

    @Test
    fun `attempting to rehydrate a match with missing creation event`() = test(timeout = timeout) {

        val (player1, player2, matchId) = List(3) { newId() }
        val attempt = runCatching {
            ticTacToeMatchEvents(matchId = matchId, firstPlayer = player1, secondPlayer = player2) {
                firstPlayer.claimedPosition(row = MID, column = CENTRE)
                secondPlayer.claimedPosition(row = BOTTOM, column = LEFT)
                firstPlayer.claimedPosition(row = TOP, column = RIGHT)
                secondPlayer.claimedPosition(row = MID, column = LEFT)
                firstPlayer.claimedPosition(row = TOP, column = LEFT)
                secondPlayer.claimedPosition(row = TOP, column = CENTRE)
                firstPlayer.claimedPosition(row = BOTTOM, column = RIGHT)
            }
        }

        assertThat(attempt).failedThrowing<IllegalStateException>()
    }

    @Test
    fun `attempting to rehydrate a match with multiple creation events`() = test(timeout = timeout) {

        val (player1, player2, matchId) = List(3) { newId() }
        val attempt = runCatching {
            ticTacToeMatchEvents(matchId = matchId, firstPlayer = player1, secondPlayer = player2) {
                match.wasCreated()
                match.wasCreated()
            }
        }

        assertThat(attempt).failedThrowing<IllegalStateException>()
    }

    @Test
    fun `a player concedes the match`() = testWithMatchAndPlayers(timeout = timeout) { player1, player2, match ->

        player1.waitForTurnAndClaimPosition(row = MID, column = CENTRE)
        player2.waitForTurnAndClaimPosition(row = BOTTOM, column = LEFT)
        player1.waitForTurnAndClaimPosition(row = TOP, column = RIGHT)
        player2.waitForTurnAndConcede()

        assertThat(match).wasWon().by(player1).becauseTheOtherPlayerConceded()
    }

    @Test
    fun `rehydrating a conceded match from its events`() = test(timeout = timeout) {

        val framework = newInMemoryEventFramework()
        val (player1, player2, matchId) = List(3) { newId() }
        val events = ticTacToeMatchEvents(matchId = matchId, firstPlayer = player1, secondPlayer = player2) {
            match.wasCreated()
            firstPlayer.claimedPosition(row = MID, column = CENTRE)
            secondPlayer.claimedPosition(row = BOTTOM, column = LEFT)
            firstPlayer.claimedPosition(row = TOP, column = RIGHT)
            secondPlayer.conceded()
        }

        framework.publishTicTacToeMatchEvents(matchId, events)
        val ticTacToe = TicTacToe.newGameEngine(framework = framework)
        val match = ticTacToe.matchWithId(matchId)

        assertThat(match).wasWon().by(player1).becauseTheOtherPlayerConceded()
    }

    @Test
    fun `a player attempts to concede outside their turn`() = testWithMatchAndPlayers { player1, player2, match ->

        player1.waitForTurnAndClaimPosition(row = TOP, column = RIGHT)
        player2.waitForTurnAndClaimPosition(row = BOTTOM, column = LEFT)

        val attempt = runCatching { player2.concede() }

        assertThat(attempt).failedThrowing(IllegalTurnException(attemptingPlayer = player2.id, currentPlayer = player1.id, matchId = match.id))
    }

    @Test
    fun `a player attempts to move after the match is already conceded`() = testWithMatchAndPlayers { player1, player2, _ ->

        player1.waitForTurnAndClaimPosition(row = MID, column = CENTRE)
        player2.concede()

        val attempt = runCatching { player1.claimPosition(row = BOTTOM, column = CENTRE) }

        assertThat(attempt).failedThrowing<AlreadyTerminatedMatchException>()
    }

    @Test
    fun `the framework records events as the match is played`() = test(timeout = timeout) {

        val framework = newInMemoryEventFramework()
        val matchId = newId()
        val player1Id = newId()
        val player2Id = newId()
        withMatchAndPlayers(matchId = matchId, framework = framework, player1Id = player1Id, player2Id = player2Id) { player1, player2, _ ->
            player1.waitForTurnAndClaimPosition(row = MID, column = CENTRE)
            player2.waitForTurnAndClaimPosition(row = BOTTOM, column = LEFT)
            player1.concede()
        }

        val matchEvents = TicTacToeMatch.Events(matchId = matchId, framework = framework)
        val events = matchEvents.history.all.toList()

        assertThat(events).hasSize(4)
        assertThat(events[0].data).isInstanceOf<MatchCreated>().all {
            prop(MatchCreated::matchId).isEqualTo(matchId)
            prop(MatchCreated::player1).isEqualTo(player1Id)
            prop(MatchCreated::player2).isEqualTo(player2Id)
        }
        assertThat(events[1].data).isInstanceOf<PositionClaimed>().all {
            prop(PositionClaimed::matchId).isEqualTo(matchId)
            prop(PositionClaimed::player).isEqualTo(player1Id)
            prop(PositionClaimed::newPlayer).isEqualTo(player2Id)
            prop(PositionClaimed::position).isEqualTo(Position(row = MID, column = CENTRE))
        }
        assertThat(events[2].data).isInstanceOf<PositionClaimed>().all {
            prop(PositionClaimed::matchId).isEqualTo(matchId)
            prop(PositionClaimed::player).isEqualTo(player2Id)
            prop(PositionClaimed::newPlayer).isEqualTo(player1Id)
            prop(PositionClaimed::position).isEqualTo(Position(row = BOTTOM, column = LEFT))
        }
        assertThat(events[3].data).isInstanceOf<MatchConceded>().all {
            prop(MatchConceded::matchId).isEqualTo(matchId)
            prop(MatchConceded::player).isEqualTo(player1Id)
            prop(MatchConceded::winner).isEqualTo(player2Id)
        }
    }

    private fun testWithMatchAndPlayers(player1Id: Id = newId(), player2Id: Id = newId(), matchId: Id = newId(), currentPlayer: Id = player1Id, framework: EventFramework = newInMemoryEventFramework(), timeout: Duration = this.timeout, scenario: suspend (player1: TicTacToePlayer, player2: TicTacToePlayer, match: TicTacToeMatch) -> Unit) = test(timeout = timeout) {

        withMatchAndPlayers(player1Id = player1Id, player2Id = player2Id, matchId = matchId, currentPlayer = currentPlayer, framework = framework, scenario = scenario)
    }

    private suspend fun withMatchAndPlayers(player1Id: Id = newId(), player2Id: Id = newId(), matchId: Id = newId(), currentPlayer: Id = player1Id, framework: EventFramework = newInMemoryEventFramework(), scenario: suspend (player1: TicTacToePlayer, player2: TicTacToePlayer, match: TicTacToeMatch) -> Unit) {

        val user1 = newUser(id = player1Id)
        val user2 = newUser(id = player2Id)
        val ticTacToe = TicTacToe.newGameEngine(framework = framework)
        val match = ticTacToe.newMatch(player1Id, player2Id, currentPlayer = currentPlayer, id = matchId)
        val player1 = user1.asPlayerIn(match)
        val player2 = user2.asPlayerIn(match)

        scenario(player1, player2, match)
    }

    private fun ticTacToeMatchEvents(firstPlayer: Id = newId(), secondPlayer: Id = newId(), matchId: Id = newId(), currentPlayer: Id = firstPlayer, build: TicTacToeMatchEventsBuilder.() -> Unit): List<TicTacToeMatch.Event> = TicTacToeMatchEventsBuilderImplementation(matchId, firstPlayer, secondPlayer, currentPlayer).also(build).events()

    private suspend fun EventFramework.publishTicTacToeMatchEvents(matchId: Id, events: Flow<TicTacToeMatch.Event>) {

        val streamId = TicTacToeMatch.EventStreamId(matchId)
        events.collect { publish(streamId, it) }
    }

    private suspend fun EventFramework.publishTicTacToeMatchEvents(matchId: Id, events: Iterable<TicTacToeMatch.Event>) = publishTicTacToeMatchEvents(matchId, events.asFlow())

    context(_: UniqueIdGenerator)
    private fun newUser(id: Id = newId()): User = UserEntity(id = id)

    private fun newInMemoryEventFramework(): EventFramework = PartitionedInMemoryEventFramework()
}

class TicTacToeMatchEventsBuilderImplementation(private val matchId: Id, private val player1: Id, private val player2: Id, private val initialPlayer: Id) : TicTacToeMatchEventsBuilder {

    private val events = mutableListOf<TicTacToeMatch.Event>()
    override val match: MatchEventsBuilder = MatchEventsBuilderImplementation()
    override val firstPlayer: PlayerEventsBuilder = PlayerEventsBuilderImplementation(player1)
    override val secondPlayer: PlayerEventsBuilder = PlayerEventsBuilderImplementation(player2)

    private inner class MatchEventsBuilderImplementation : MatchEventsBuilder {

        context(_: UniqueIdGenerator, _: TimeGenerator, _: RandomGenerator)
        override fun wasCreated(player1: Id, player2: Id, id: Id, timestamp: Instant) {

            ensureMatchWasNotCreatedAlready()
            events += MatchCreated(matchId = matchId, player1 = player1, player2 = player2, currentPlayer = initialPlayer, id = id, timestamp = timestamp)
        }

        context(_: UniqueIdGenerator, _: TimeGenerator, _: RandomGenerator)
        override fun wasCreated(id: Id, timestamp: Instant) = wasCreated(player1 = player1, player2 = player2, id = id, timestamp = timestamp)

        private fun ensureMatchWasNotCreatedAlready() = check(events.isEmpty()) { "Can't create another match with ID ${matchId.stringValue}, as one already exists" }
    }

    private inner class PlayerEventsBuilderImplementation(override val playerId: Id) : PlayerEventsBuilder {

        private val otherPlayer: Id get() = if (playerId == player1) player2 else player1
        private val currentPlayer get() = events.filterIsInstance<WithPlayerChange>().last().newPlayer

        context(_: UniqueIdGenerator, _: TimeGenerator)
        override fun claimedPosition(position: Position, id: Id, timestamp: Instant) {

            ensureMatchExists()
            require(playerId == currentPlayer) { "Player with ID ${playerId.stringValue} can't play, as the turn belongs to player with ID ${currentPlayer.stringValue}" }
            events += PositionClaimed(matchId = matchId, position = position, player = playerId, newPlayer = otherPlayer, id = id, timestamp = timestamp)
        }

        context(_: UniqueIdGenerator, _: TimeGenerator)
        override fun conceded(id: Id, timestamp: Instant) {

            ensureMatchExists()
            require(playerId == currentPlayer) { "Player with ID ${playerId.stringValue} can't play, as the turn belongs to player with ID ${currentPlayer.stringValue}" }
            events += MatchConceded(matchId = matchId, winner = otherPlayer, player = playerId, timestamp = timestamp, id = id)
        }

        private fun ensureMatchExists() = check(events.filterIsInstance<MatchCreated>().singleOrNull() != null) { "Can't claim a position, because the match with ID ${matchId.stringValue} isn't created yet" }
    }

    fun events(): List<TicTacToeMatch.Event> = events
}

context(ids: UniqueIdGenerator, time: TimeGenerator)
fun PlayerEventsBuilder.claimedPosition(row: Row, column: Column, id: Id = ids.newId(), timestamp: Instant = time.now()) = claimedPosition(Position(row, column), id, timestamp)

interface PlayerEventsBuilder {

    val playerId: Id

    context(ids: UniqueIdGenerator, time: TimeGenerator)
    fun claimedPosition(position: Position, id: Id = ids.newId(), timestamp: Instant = time.now())

    context(ids: UniqueIdGenerator, time: TimeGenerator)
    fun conceded(id: Id = ids.newId(), timestamp: Instant = time.now())
}

interface MatchEventsBuilder {

    context(ids: UniqueIdGenerator, time: TimeGenerator, _: RandomGenerator)
    fun wasCreated(player1: Id, player2: Id, id: Id = ids.newId(), timestamp: Instant = time.now())

    context(ids: UniqueIdGenerator, time: TimeGenerator, _: RandomGenerator)
    fun wasCreated(id: Id = ids.newId(), timestamp: Instant = time.now())
}

interface TicTacToeMatchEventsBuilder {

    val match: MatchEventsBuilder
    val firstPlayer: PlayerEventsBuilder
    val secondPlayer: PlayerEventsBuilder
}

fun Assert<TicTacToeMatch>.wasWon(): Assert<Victory> = transform("victory") { match ->

    if (match.state is Victory) return@transform match.state as Victory
    expected("to be a victory, but was ${show(match.state)}")
}

fun Assert<TicTacToeMatch>.wasADraw(): Assert<Draw> = transform("draw") { match ->

    if (match.state === Draw) return@transform match.state as Draw
    expected("to be a draw, but was ${show(match.state)}")
}

fun Assert<Victory>.by(player: TicTacToePlayer): Assert<Victory> = by(player.id)

fun Assert<Victory>.by(player: Id): Assert<Victory> = transform("victory") { victory ->

    if (victory.winner == player) return@transform victory
    expected("to belong to player ${show(player)}, but belonged to player ${show(victory.winner)}", player, victory.winner)
}

fun Assert<Victory>.withSequence(sequence: WinningSequence): Assert<Victory.Regular> = transform("regular victory") { victory ->

    if (victory is Victory.Regular && victory.sequence == sequence) return@transform victory
    expected("to be regular with sequence ${show(sequence)}, but was ${show(victory)}")
}

fun Assert<Victory>.becauseTheOtherPlayerConceded(): Assert<Victory.Conceded> = transform("conceded victory") { victory ->

    if (victory is Victory.Conceded) return@transform victory
    expected("to be conceded, but was ${show(victory)}")
}

context(generator: CoreDataGenerator)
fun TicTacToe.Companion.newGameEngine(framework: EventFramework): TicTacToe = TicTacToeGameEngine(framework = framework, coreDataGenerator = generator)

data class UserEntity(override val id: Id) : User

interface User : Identifiable

fun User.asPlayerIn(match: TicTacToeMatch): TicTacToePlayer = UserAsTicTacToePlayer(user = this, match = match)

data class UserAsTicTacToePlayer(private val user: User, private val match: TicTacToeMatch) : TicTacToePlayer, Identifiable by user {

    override suspend fun claimPosition(position: Position) = match.claimPositionForPlayer(player = id, position = position)

    override suspend fun concede() = match.concedeByPlayer(player = id)

    override suspend fun waitForTurn(): Unit = coroutineScope {

        if (match.currentPlayer != id) {
            match.events.awaitFirst<WithPlayerChange> { event -> event.newPlayer == id }
        }
    }
}

suspend inline fun <reified EVENT> EventStream<Event>.awaitFirst(noinline filter: (EVENT) -> Boolean = { true }): EVENT = subscribe.filterIsInstance<EVENT>().filter(filter).first()

interface TicTacToePlayer : Identifiable {

    suspend fun claimPosition(position: Position)

    suspend fun concede()

    suspend fun waitForTurn()
}

suspend fun TicTacToePlayer.claimPosition(row: Row, column: Column) = claimPosition(Position(row, column))

suspend fun TicTacToePlayer.waitForTurnAndClaimPosition(row: Row, column: Column) = waitForTurnAndClaimPosition(position = Position(row, column))

suspend fun TicTacToePlayer.waitForTurnAndClaimPosition(position: Position) {

    waitForTurn()
    claimPosition(position)
}

suspend fun TicTacToePlayer.waitForTurnAndConcede() {

    waitForTurn()
    concede()
}

class TicTacToeGameEngine(private val framework: EventFramework, coreDataGenerator: CoreDataGenerator) : TicTacToe, CoreDataGenerator by coreDataGenerator {

    context(_: RandomGenerator, ids: UniqueIdGenerator)
    override suspend fun newMatch(player1: Id, player2: Id, currentPlayer: Id, id: Id): TicTacToeMatch {

        val time = now()
        val matchCreatedEvent = MatchCreated(matchId = id, currentPlayer = currentPlayer, player1 = player1, player2 = player2, id = ids.newId(), timestamp = time)
        val matchEventStream = TicTacToeMatch.Events(matchId = id, framework = framework)
        matchEventStream.publish(event = matchCreatedEvent)
        return ProjectedTicTacToeMatch(events = matchEventStream, coreDataGenerator = this)
    }

    override fun matchWithId(id: Id): TicTacToeMatch {

        val matchEvents = TicTacToeMatch.Events(matchId = id, framework = framework)
        return ProjectedTicTacToeMatch(events = matchEvents, coreDataGenerator = this)
    }
}

interface EventStreamId {

    val entityName: Name

    val entityId: Name

    val name: Name get() = "${entityName.value}-${entityId.value}".let(::Name)
}

sealed class TicTacToeMatchException(message: String) : Exception(message)

sealed class IllegalMoveException(message: String) : TicTacToeMatchException(message)

data class PositionAlreadyClaimedException(val position: Position, val attemptingPlayer: Id, val controllingPlayerId: Id, val matchId: Id) : IllegalMoveException("Player with ID ${attemptingPlayer.stringValue} in match with ID ${matchId.stringValue} cannot claim position ${position}, as it belongs to player with ID ${controllingPlayerId.stringValue}")

data class IllegalTurnException(val attemptingPlayer: Id, val currentPlayer: Id, val matchId: Id) : IllegalMoveException("Player ${attemptingPlayer.stringValue} cannot move, because the current player in match with ID ${matchId.stringValue} is ${currentPlayer.stringValue}")

data class AlreadyTerminatedMatchException(val attemptingPlayer: Id, val matchId: Id, val matchState: TicTacToeMatch.State) : IllegalMoveException("Player ${attemptingPlayer.stringValue} cannot move, because the match with ID ${matchId.stringValue} is already terminated with state ${matchState}")

class ProjectedTicTacToeMatch(override val events: TicTacToeMatch.Events, coreDataGenerator: CoreDataGenerator) : TicTacToeMatch, CoreDataGenerator by coreDataGenerator {

    private val projection: Projection get() = runBlocking { events.history.all.map { it.data }.toList().let(::Projection) }
    override val currentPlayer get() = projection.currentPlayer
    override val player1 get() = projection.player1
    override val player2 get() = projection.player2
    override val id get() = projection.matchId
    override val state: TicTacToeMatch.State get() = projection.state

    override suspend fun claimPositionForPlayer(player: Id, position: Position) {

        ensurePlayerCanMove(player)
        ensurePositionIsNotAlreadyClaimed(position = position, player = player)
        val positionClaimed = PositionClaimed(id = newId(), timestamp = clock.now(), matchId = id, player = player, position = position, newPlayer = otherPlayer(player))
        events.publish(positionClaimed)
    }

    override suspend fun concedeByPlayer(player: Id) {

        ensurePlayerCanMove(player)
        val matchConceded = MatchConceded(id = newId(), timestamp = clock.now(), matchId = id, winner = otherPlayer(player), player = player)
        events.publish(matchConceded)
    }

    private fun ensurePositionIsNotAlreadyClaimed(position: Position, player: Id) {
        projection.controllingPlayerForPosition(position)?.let { throw PositionAlreadyClaimedException(position = position, attemptingPlayer = player, controllingPlayerId = it, matchId = id) }
    }

    private fun ensurePlayerCanMove(player: Id) {
        ensureStateIsOpen(attemptingPlayer = player)
        ensureTheTurnBelongsToThePlayer(player)
    }

    private fun ensureTheTurnBelongsToThePlayer(player: Id) = check(player == currentPlayer) { throw IllegalTurnException(attemptingPlayer = player, currentPlayer = otherPlayer(player), matchId = id) }

    private fun otherPlayer(player: Id) = if (player == player1) player2 else player1

    private fun ensureStateIsOpen(attemptingPlayer: Id) = check(state is Open) { throw AlreadyTerminatedMatchException(attemptingPlayer = attemptingPlayer, matchState = state, matchId = id) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProjectedTicTacToeMatch

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString() = "ProjectedTicTacToeMatch(id=$id, state=$state)"

    private data class Projection(val events: List<TicTacToeMatch.Event>) {

        private val creation: MatchCreated by lazy { events.filterIsInstance<MatchCreated>().single() }
        val player1 get() = creation.player1
        val player2 get() = creation.player2
        val matchId get() = creation.matchId
        val currentPlayer get() = events.filterIsInstance<WithPlayerChange>().last().newPlayer
        val state: TicTacToeMatch.State by lazy { state(events) }

        fun controllingPlayerForPosition(position: Position): Id? = events.filterIsInstance<PositionClaimed>().singleOrNull { it.position == position }?.player

        private fun state(events: List<TicTacToeMatch.Event>): TicTacToeMatch.State = events.projectedState()

        private fun List<TicTacToeMatch.Event>.projectedState(): TicTacToeMatch.State {

            filterIsInstance<MatchConceded>().singleOrNull()?.let { return Victory.Conceded(winner = it.winner) }
            val positionsClaimed = filterIsInstance<PositionClaimed>()
            val moves = positionsClaimed.associateBy(PositionClaimed::position)
            possibleWinningSequences.forEach { winningSequence ->
                winningSequence.exclusiveControllingPlayerOrNull(moves)?.let { winner ->
                    return Victory.Regular(winner = winner, sequence = winningSequence)
                }
            }
            if (moves.size == TicTacToe.GRID_POSITIONS_COUNT) return Draw
            return Open(currentPlayer = (positionsClaimed.lastOrNull() ?: creation).newPlayer)
        }

        private fun WinningSequence.isCoveredBy(moves: Map<Position, PositionClaimed>) = positions.all { it in moves.keys }

        private fun WinningSequence.controllingPlayersFrom(moves: Map<Position, PositionClaimed>) = positions.map { moves[it]!!.player }.toSet()

        private fun WinningSequence.exclusiveControllingPlayerOrNull(moves: Map<Position, PositionClaimed>) = if (isCoveredBy(moves)) controllingPlayersFrom(moves).singleOrNull() else null
    }
}

interface TicTacToe {

    context(random: RandomGenerator, ids: UniqueIdGenerator)
    suspend fun newMatch(player1: Id, player2: Id, currentPlayer: Id = setOf(player1, player2).random(random = random.random), id: Id = ids.newId()): TicTacToeMatch

    data class Position(val row: Row, val column: Column) {

        enum class Row {
            BOTTOM, MID, TOP
        }

        enum class Column {
            LEFT, CENTRE, RIGHT
        }
    }

    sealed class WinningSequence(val positions: Set<Position>) {

        data object LeftColumn : WinningSequence(Row.entries.map { Position(it, LEFT) }.toSet())
        data object CentreColumn : WinningSequence(Row.entries.map { Position(it, CENTRE) }.toSet())
        data object RightColumn : WinningSequence(Row.entries.map { Position(it, RIGHT) }.toSet())
        data object TopRow : WinningSequence(Column.entries.map { Position(TOP, it) }.toSet())
        data object MiddleRow : WinningSequence(Column.entries.map { Position(MID, it) }.toSet())
        data object BottomRow : WinningSequence(Column.entries.map { Position(BOTTOM, it) }.toSet())
        data object LeftRightDiagonal : WinningSequence(setOf(Position(TOP, LEFT), Position(MID, CENTRE), Position(BOTTOM, RIGHT)))
        data object RightLeftDiagonal : WinningSequence(setOf(Position(TOP, RIGHT), Position(MID, CENTRE), Position(BOTTOM, LEFT)))
    }

    companion object {
        const val GRID_POSITIONS_COUNT = 9

        val possibleWinningSequences: Set<WinningSequence> = setOf(LeftColumn, CentreColumn, RightColumn, TopRow, MiddleRow, BottomRow, LeftRightDiagonal, RightLeftDiagonal)
    }

    fun matchWithId(id: Id): TicTacToeMatch
}

interface TicTacToeMatch : Identifiable {

    val currentPlayer: Id
    val player1: Id
    val player2: Id

    val state: State
    val events: Events

    suspend fun claimPositionForPlayer(player: Id, position: Position)

    suspend fun concedeByPlayer(player: Id)

    sealed class State {

        data class Open(val currentPlayer: Id) : State()

        sealed class Finished : State()

        data object Draw : Finished()

        sealed class Victory(open val winner: Id) : Finished() {

            data class Regular(override val winner: Id, val sequence: WinningSequence) : Victory(winner)

            data class Conceded(override val winner: Id) : Victory(winner)
        }
    }

    sealed interface Event : sollecitom.exercises.edttt.Event {

        data class MatchCreated(override val id: Id, override val timestamp: Instant, val matchId: Id, val player1: Id, val player2: Id, val currentPlayer: Id) : Event, WithPlayerChange {

            init {
                require(currentPlayer == player1 || currentPlayer == player2) { "Current player must be either player1 (${player1.stringValue}) or player2 (${player2.stringValue}), but was ${currentPlayer.stringValue}" }
            }

            override val newPlayer: Id get() = currentPlayer
        }

        data class PositionClaimed(override val id: Id, override val timestamp: Instant, val matchId: Id, val player: Id, val position: Position, override val newPlayer: Id) : Event, WithPlayerChange {

            init {
                require(newPlayer != player) { "Player must change after a position is claimed" }
            }
        }

        data class MatchConceded(override val id: Id, override val timestamp: Instant, val matchId: Id, val winner: Id, val player: Id) : Event

        interface WithPlayerChange {

            val newPlayer: Id
        }
    }

    data class EventStreamId(val matchId: Id) : sollecitom.exercises.edttt.EventStreamId {

        override val entityName get() = Companion.entityName
        override val entityId = matchId.stringValue.let(::Name)

        companion object {
            val entityName = "tic-tac-toe-match".let(::Name)
        }
    }

    class Events(private val streamId: EventStreamId, private val framework: EventFramework) : EventStream<Event> by framework.eventStream(streamId = streamId) {

        constructor(matchId: Id, framework: EventFramework) : this(streamId = EventStreamId(matchId = matchId), framework = framework)

        suspend fun publish(event: Event) = framework.publish(streamId = streamId, event = event)
    }

    companion object
}

interface Event : Identifiable, Timestamped

class PartitionedInMemoryEventFramework : EventFramework {

    private val storageById = mutableMapOf<EventStreamId, InMemoryEventStreamStorage>()

    override suspend fun publish(streamIds: Set<EventStreamId>, event: Event) {

        streamIds.forEach { streamId ->
            val storage = storageWithId(streamId)
            storage.record(event, streamIds)
        }
    }

    private fun storageWithId(streamId: EventStreamId) = storageById.getOrPut(streamId, ::InMemoryEventStreamStorage)

    override fun <EVENT : Event> eventStream(streamId: EventStreamId, eventType: KClass<EVENT>): EventStream<EVENT> = storageWithId(streamId).withEventType(eventType)

    private class InMemoryEventStreamStorage {

        private val storage = mutableListOf<EventWithMetadata<Event>>()
        private val flow = MutableSharedFlow<EventWithMetadata<Event>>(replay = Int.MAX_VALUE)
        private var offset = -1L

        suspend fun record(event: Event, streamIds: Set<EventStreamId>) {

            val nextEvent = synchronized(this) {
                offset++
                EventWithMetadata(data = event, metadata = EventMetadata(offset = offset, streamIds = streamIds))
            }
            storage += nextEvent
            flow.emit(nextEvent)
        }

        fun <EVENT : Event> withEventType(eventType: KClass<EVENT>): EventStream<EVENT> = FilteredEventStream(eventType = eventType)

        private inner class FilteredEventStream<out EVENT : Event>(private val eventType: KClass<EVENT>) : EventStream<EVENT> {

            override val history: EventStream.History<EVENT> = EventStreamHistoryAdapter { storage.asFlow().filtered() }
            override val subscribe: Flow<EventWithMetadata<EVENT>> = flow.filtered()

            @Suppress("UNCHECKED_CAST")
            private fun Flow<EventWithMetadata<Event>>.filtered() = filter { eventType.isInstance(it.data) }.map { it as EventWithMetadata<EVENT> }
        }
    }
}

interface EventFramework {

    suspend fun publish(streamIds: Set<EventStreamId>, event: Event)

    fun <EVENT : Event> eventStream(streamId: EventStreamId, eventType: KClass<EVENT>): EventStream<EVENT>
}

suspend fun EventFramework.publish(streamId: EventStreamId, event: Event) = publish(streamIds = setOf(streamId), event = event)

inline fun <reified EVENT : Event> EventFramework.eventStream(streamId: EventStreamId): EventStream<EVENT> = eventStream(streamId, EVENT::class)

interface EventStream<out EVENT : Event> {

    val history: History<EVENT>

    val subscribe: Flow<EventWithMetadata<EVENT>>

    interface History<out EVENT : Event> {

        val all: Flow<EventWithMetadata<EVENT>>
    }
}

data class EventWithMetadata<out EVENT : Event>(val data: EVENT, val metadata: EventMetadata)

data class EventMetadata(val offset: Long, val streamIds: Set<EventStreamId>)

class EventStreamHistoryAdapter<out EVENT : Event>(private val loadAll: () -> Flow<EventWithMetadata<EVENT>>) : EventStream.History<EVENT> {

    override val all: Flow<EventWithMetadata<EVENT>> get() = loadAll()
}