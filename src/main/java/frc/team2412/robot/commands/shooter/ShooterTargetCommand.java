package frc.team2412.robot.commands.shooter;

import static frc.team2412.robot.subsystem.ShooterSubsystem.ShooterConstants;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.team2412.robot.subsystem.ShooterSubsystem;
import frc.team2412.robot.util.ShooterDataDistancePoint;
import frc.team2412.robot.util.ShooterVision;

public class ShooterTargetCommand extends CommandBase {
    private final ShooterSubsystem shooter;
    private final ShooterVision vision;

    public ShooterTargetCommand(ShooterSubsystem shooter, ShooterVision vision) {
        this.shooter = shooter;
        this.vision = vision;
        addRequirements(shooter);
    }

    @Override
    public void execute() {
        double distance = vision.getDistance() + shooter.getDistanceBias();
        double yaw = vision.getYaw() + shooter.getTurretAngleBias();
        ShooterDataDistancePoint shooterData = ShooterConstants.dataPoints.getInterpolated(distance);
        shooter.setHoodAngle(shooterData.getAngle());
        shooter.setFlywheelRPM(shooterData.getRPM());
        shooter.updateTurretAngle(yaw);
    }

    @Override
    public void end(boolean interrupted) {
        shooter.stopFlywheel();
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
