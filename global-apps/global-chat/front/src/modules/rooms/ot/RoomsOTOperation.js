class RoomsOTOperation {
  constructor(rooms) {
    this.rooms = rooms;
  }

  apply(state) {
    this.rooms.forEach(el => {
      if (el.remove) {
        state.delete(el.id);
      } else {
        state.set(el.id, {
          participants: el.participants
        })
      }
    });

    return state;
  }

  isEmpty() {
    return this.rooms.every(el => el.participants.length === 0);
  }

  invert() {
    return new RoomsOTOperation(this.rooms.map(room => ({
      id: room.id,
      participants: room.participants,
      remove: !room.remove
    })));
  }

}

export default RoomsOTOperation;
