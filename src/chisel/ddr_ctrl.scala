import Chisel._

class DDRControlModule extends Module {
  val io = new Bundle {
    val init_calib_complete = Bool(INPUT)
    val mig_data_valid = Bool(INPUT)
    val mig_rdy = Bool(INPUT)
    val mig_wdf_rdy = Bool(INPUT)
    val data_from_mig = UInt(INPUT, 128)

    val ram_en = Bool(INPUT)
    val ram_write = Bool(INPUT)
    val ram_addr = UInt(INPUT, 30)
    val data_to_ram = UInt(INPUT, 256)

    val cmd_to_mig = UInt(OUTPUT, 3)
    val app_en = Bool(OUTPUT)
    val ram_rdy = Bool(OUTPUT)

    val app_wdf_wren = Bool(OUTPUT)
    val app_wdf_end = Bool(OUTPUT)

    val addr_to_mig = UInt(OUTPUT, 27)
    val data_to_mig = UInt(OUTPUT, 128)
    val data_to_cpu = UInt(OUTPUT, 256)
  }
  val (idle :: w1req :: w1wait :: w2req :: w2wait :: r1req :: r1wait :: r2req ::
    r2wait :: finish :: Nil) = Enum(UInt(), 10)
  val state = Reg(init = idle)
  val counter = Reg(init = UInt(0, 4))
  val buffer = Reg(init = UInt(0, 256))

  io.cmd_to_mig := UInt(1)
  io.app_en := UInt(0)
  io.ram_rdy := UInt(0)
  io.data_to_mig := UInt(0)
  io.addr_to_mig := UInt(0)
  io.app_wdf_wren := UInt(0);
  io.app_wdf_end := UInt(0);

  when (io.init_calib_complete) {
    counter := counter + UInt(1)
    when (state === idle) {
      counter := UInt(0)
      when (io.ram_en & ~io.ram_write) { state := r1req }
      when (io.ram_en & io.ram_write) { state := w1req }
    }
    when (state === w1req) {
      io.cmd_to_mig := UInt(0)
      io.app_en := UInt(1)
      io.app_wdf_wren := UInt(1);
      io.app_wdf_end := UInt(1);
      io.addr_to_mig := Cat(io.ram_addr(25, 3), UInt(0, 5))
      io.data_to_mig := io.data_to_ram(127, 0)
      when (io.mig_rdy & io.mig_wdf_rdy & counter =/= UInt(0)) {
        state := w1wait
      }
    }
    when (state === w1wait) {
      when (io.mig_rdy) {
        counter := UInt(0)
        state := w2req
      }
    }
    when (state === w2req) {
      io.cmd_to_mig := UInt(0)
      io.app_en := UInt(1)
      io.app_wdf_wren := UInt(1);
      io.app_wdf_end := UInt(1);
      io.addr_to_mig := Cat(io.ram_addr(25, 3), UInt(16, 5))
      io.data_to_mig := io.data_to_ram(255, 128)
      when (io.mig_rdy & io.mig_wdf_rdy & counter =/= UInt(0)) {
        state := finish
      }
    }
    when (state === r1req) {
      io.app_en := UInt(1)
      io.addr_to_mig := Cat(io.ram_addr(25, 3), UInt(0, 5))
      when (io.mig_rdy & counter =/= UInt(0)) {
        state := r1wait
      }
    }
    when (state === r1wait) {
      when (io.mig_rdy & io.mig_data_valid) {
        counter := UInt(0)
        state := r2req
        buffer(127, 0) := io.data_from_mig
      }
    }
    when (state === r2req) {
      io.app_en := UInt(1)
      io.addr_to_mig := Cat(io.ram_addr(25, 3), UInt(16, 5))
      when (io.mig_rdy & counter =/= UInt(0)) {
        state := r2wait
      }
    }
    when (state === r2wait) {
      when (io.mig_rdy & io.mig_data_valid) {
        state := finish
        buffer(255, 128) := io.data_from_mig
      }
    }
    when (state === finish) {
      state := idle
    }
  }

  io.ram_rdy := (state === idle & io.ram_en =/= UInt(1)) | (state === finish)
  /* | (state === finish)*/
  io.data_to_cpu := buffer
}

class HelloModuleTests(c: DDRControlModule) extends Tester(c) {
  step(1)
}

object hello {
  def main(args: Array[String]): Unit = {
    chiselMainTest(Array[String]("--backend", "v", "--genHarness"),
      () => Module(new DDRControlModule())){c => new HelloModuleTests(c)}
  }
}
