import RoomsOTOperation from './RoomsOTOperation';

const roomsSerializer = {
  serialize(value) {
    return value.rooms;
  },

  deserialize(value) {
    return new RoomsOTOperation(value);
  }
};

export default roomsSerializer;
