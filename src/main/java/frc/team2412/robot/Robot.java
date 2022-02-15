// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.team2412.robot;

import static java.lang.Thread.sleep;

import org.frcteam2910.common.math.RigidTransform2;
import org.frcteam2910.common.robot.UpdateManager;

import edu.wpi.first.hal.simulation.DriverStationDataJNI;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.team2412.robot.Subsystems.SubsystemConstants;
import frc.team2412.robot.commands.shooter.ShooterResetEncodersCommand;
import frc.team2412.robot.subsystem.DrivebaseSubsystem;
import frc.team2412.robot.subsystem.TestingSubsystem;
import frc.team2412.robot.util.AutonomousTrajectories;
import frc.team2412.robot.util.AutonomousChooser;
import io.github.oblarg.oblog.Loggable;
import io.github.oblarg.oblog.Logger;

public class Robot extends TimedRobot implements Loggable {
    /**
     * Singleton Stuff
     */
    private static Robot instance = null;

    enum RobotType {
        COMPETITION, AUTOMATED_TEST
    }

    public static Robot getInstance(RobotType type) {
        if (instance == null)
            instance = new Robot(type);
        return instance;
    }

    public Controls controls;
    public Subsystems subsystems;
    public Hardware hardware;

    private UpdateManager updateManager;
    private AutonomousChooser testModeChooser;
    final private RobotType robotType;

    private Thread controlAuto;

    public TestingSubsystem testingSubsystem;

    Robot(RobotType type) {
        System.out.println("Robot type: " + (type.equals(RobotType.AUTOMATED_TEST) ? "AutomatedTest" : "Competition"));
        instance = this;
        robotType = type;
    }

    // TODO add other override methods

    public Field2d field = new Field2d();;

    @Override
    public void startCompetition() {
        if (!robotType.equals(RobotType.AUTOMATED_TEST)) {
            super.startCompetition();
        } else {
            try {
                super.startCompetition();
            } catch (Throwable throwable) {
                Throwable cause = throwable.getCause();
                if (cause != null) {
                    // We're about to exit, so overwriting the param is fine
                    // noinspection AssignmentToCatchBlockParameter
                    throwable = cause;
                }
                DriverStation.reportError(
                        "Unhandled exception: " + throwable.toString(), throwable.getStackTrace());

                try {
                    sleep(2000);
                } catch (InterruptedException ignored) {
                }
                java.lang.System.exit(-1);
            }
        }
    }

    @Override
    public void robotInit() {
        hardware = new Hardware();
        subsystems = new Subsystems(hardware);
        controls = new Controls(subsystems);
        updateManager = new UpdateManager(
                subsystems.drivebaseSubsystem);
        updateManager.startLoop(5.0e-3);

        // Create and push Field2d to SmartDashboard.
        SmartDashboard.putData(field);

        testModeChooser = new AutonomousChooser(subsystems,
                new AutonomousTrajectories(DrivebaseSubsystem.DriveConstants.TRAJECTORY_CONSTRAINTS));
        Logger.configureLoggingAndConfig(subsystems, false);

        CommandScheduler.getInstance()
                .onCommandInitialize(
                        command -> System.out.println("Command initialized: " + command.getName()));
        CommandScheduler.getInstance()
                .onCommandInterrupt(
                        command -> System.out.println("Command interrupted: " + command.getName()));
        CommandScheduler.getInstance()
                .onCommandFinish(
                        command -> System.out.println("Command finished: " + command.getName()));

        if (robotType.equals(RobotType.AUTOMATED_TEST)) {
            controlAuto = new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Waiting two seconds for robot to finish startup");
                    try {
                        sleep(2000);
                    } catch (InterruptedException ignored) {
                    }

                    System.out.println("Enabling autonomous mode and waiting 10 seconds");
                    DriverStationDataJNI.setAutonomous(true);
                    DriverStationDataJNI.setEnabled(true);

                    try {
                        sleep(10000);
                    } catch (InterruptedException ignored) {
                    }

                    System.out.println("Disabling robot and waiting two seconds");
                    DriverStationDataJNI.setEnabled(false);

                    try {
                        sleep(2000);
                    } catch (InterruptedException ignored) {
                    }

                    System.out.println("Ending competition");
                    suppressExitWarning(true);
                    endCompetition();
                }
            });
            controlAuto.start();
        }
    }

    @Override
    public void testInit() {
        testModeChooser.getCommand().schedule();
    }

    @Override
    public void robotPeriodic() {
        Logger.updateEntries();
        CommandScheduler.getInstance().run();
    }

    @Override
    public void autonomousInit() {

        subsystems.drivebaseSubsystem.resetPose(RigidTransform2.ZERO);

        testModeChooser.getCommand().schedule();
        if (SubsystemConstants.SHOOTER_ENABLED) {
            new ShooterResetEncodersCommand(subsystems.shooterSubsystem).schedule();
        }
    }

    @Override
    public void autonomousExit() {
        CommandScheduler.getInstance().cancelAll();
    }
}
