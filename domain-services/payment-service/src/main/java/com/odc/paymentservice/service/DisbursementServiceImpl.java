package com.odc.paymentservice.service;

import com.odc.common.constant.PaymentConstant;
import com.odc.common.constant.Role;
import com.odc.common.constant.Status;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.paymentservice.dto.request.CreateDisbursementRequest;
import com.odc.paymentservice.dto.request.MilestoneDisbursement;
import com.odc.paymentservice.dto.request.MilestoneDisbursementRequest;
import com.odc.paymentservice.dto.response.DisbursementCalculationResponse;
import com.odc.paymentservice.dto.response.DisbursementResponse;
import com.odc.paymentservice.dto.response.LeaderDisbursementInfo;
import com.odc.paymentservice.entity.Disbursement;
import com.odc.paymentservice.entity.SystemConfig;
import com.odc.paymentservice.entity.Transaction;
import com.odc.paymentservice.entity.Wallet;
import com.odc.paymentservice.repository.DisbursementRepository;
import com.odc.paymentservice.repository.SystemConfigRepository;
import com.odc.paymentservice.repository.TransactionRepository;
import com.odc.paymentservice.repository.WalletRepository;
import com.odc.projectservice.v1.GetLeaderInfoRequest;
import com.odc.projectservice.v1.GetLeaderInfoResponse;
import com.odc.projectservice.v1.LeaderInfo;
import com.odc.projectservice.v1.ProjectServiceGrpc;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisbursementServiceImpl implements DisbursementService {
    private final SystemConfigRepository systemConfigRepository;
    private final ManagedChannel projectServiceChannel;
    private final DisbursementRepository disbursementRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public DisbursementCalculationResponse calculatePreview(UUID milestoneId, BigDecimal totalAmount) {
        SystemConfig systemConfig = systemConfigRepository
                .findByName("fee-distribution")
                .orElseThrow(() -> new BusinessException("Không tìm thấy cấu hình fee-distribution"));

        Map<String, Object> properties = systemConfig.getProperties();
        BigDecimal talentShareRate = new BigDecimal(properties.get("talentShareRate").toString());
        BigDecimal mentorShareRate = new BigDecimal(properties.get("mentorShareRate").toString());
        BigDecimal systemFeeRate = new BigDecimal(properties.get("systemFeeRate").toString());

        BigDecimal systemFee = totalAmount.multiply(systemFeeRate);
        BigDecimal mentorShare = totalAmount.multiply(mentorShareRate);
        BigDecimal talentShare = totalAmount.multiply(talentShareRate);

        ProjectServiceGrpc.ProjectServiceBlockingStub projectServiceBlockingStub = ProjectServiceGrpc.newBlockingStub(projectServiceChannel);
        GetLeaderInfoResponse leaderInfoResponse = projectServiceBlockingStub.getLeaderInfo(GetLeaderInfoRequest
                .newBuilder()
                .setMilestoneId(milestoneId.toString())
                .build());

        LeaderInfo mentor = null;
        LeaderInfo talent = null;

        for (LeaderInfo leader : leaderInfoResponse.getLeadersList()) {
            if (Role.MENTOR.toString().equals(leader.getRoleInProject())) {
                mentor = leader;
            } else if (Role.TALENT.toString().equals(leader.getRoleInProject())) {
                talent = leader;
            }
        }

        // Nếu không tìm thấy mentor hoặc talent thì throw exception
        if (mentor == null) {
            throw new BusinessException("Không tìm thấy leader MENTOR cho milestone");
        }
        if (talent == null) {
            throw new BusinessException("Không tìm thấy leader TALENT cho milestone");
        }

        // Tạo đối tượng LeaderDisbursementInfo cho MENTOR
        LeaderDisbursementInfo mentorObj = LeaderDisbursementInfo.builder()
                .userId(mentor.getUserId())
                .fullName(mentor.getFullName())
                .email(mentor.getEmail())
                .avatarUrl(mentor.getAvatarUrl())
                .isLeader(true)
                .roleInProject(Role.MENTOR.toString())
                .amount(mentorShare)
                .build();

        // Tạo đối tượng LeaderDisbursementInfo cho TALENT
        LeaderDisbursementInfo talentObj = LeaderDisbursementInfo.builder()
                .userId(talent.getUserId())
                .fullName(talent.getFullName())
                .email(talent.getEmail())
                .avatarUrl(talent.getAvatarUrl())
                .isLeader(true)
                .roleInProject(Role.TALENT.toString())
                .amount(talentShare)
                .build();

        // Trả về DisbursementCalculationResponse
        return DisbursementCalculationResponse.builder()
                .milestoneId(milestoneId.toString())
                .totalAmount(totalAmount)
                .systemFee(systemFee)
                .mentorLeader(mentorObj)
                .talentLeader(talentObj)
                .status(Status.PENDING.toString())
                .build();
    }

    @Override
    public ApiResponse<Map<String, String>> calculateDisbursement(CreateDisbursementRequest request) {
        SystemConfig systemConfig = systemConfigRepository
                .findByName("fee-distribution")
                .orElseThrow(() -> new BusinessException("Không tìm thấy cấu hình fee-distribution"));

        Map<String, Object> properties = systemConfig.getProperties();
        BigDecimal talentShareRate = new BigDecimal(properties.get("talentShareRate").toString());
        BigDecimal mentorShareRate = new BigDecimal(properties.get("mentorShareRate").toString());
        BigDecimal systemFeeRate = new BigDecimal(properties.get("systemFeeRate").toString());

        BigDecimal systemFee = request.getTotalAmount().multiply(systemFeeRate);
        BigDecimal mentorShare = request.getTotalAmount().multiply(mentorShareRate);
        BigDecimal talentShare = request.getTotalAmount().multiply(talentShareRate);

        Disbursement disbursement = Disbursement
                .builder()
                .projectId(request.getProjectId())
                .milestoneId(request.getMilestoneId())
                .totalAmount(request.getTotalAmount())
                .systemFee(systemFee)
                .mentorLeaderId(request.getMentorLeaderId())
                .mentorAmount(mentorShare)
                .talentLeaderId(request.getTalentLeaderId())
                .talentAmount(talentShare)
                .status(Status.PENDING.toString())
                .build();

        disbursementRepository.save(disbursement);

        return ApiResponse.success(Map.of("disbursementId", disbursement.getId().toString()));
    }

    @Override
    public DisbursementCalculationResponse executeDisbursement(UUID disbursementId) {
        Disbursement disbursement = disbursementRepository.findById(disbursementId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy kết quả để thực thi phân bổ tiền với id: " + disbursementId));

        if (!Status.PENDING.toString().equals(disbursement.getStatus())) {
            throw new BusinessException("Tiền đã được phân bổ về ví các team leader");
        }

        ProjectServiceGrpc.ProjectServiceBlockingStub projectServiceBlockingStub = ProjectServiceGrpc.newBlockingStub(projectServiceChannel);
        GetLeaderInfoResponse leaderInfoResponse = projectServiceBlockingStub.getLeaderInfo(GetLeaderInfoRequest
                .newBuilder()
                .setMilestoneId(disbursement.getMilestoneId().toString())
                .build());

        Wallet systemWallet = walletRepository.findByOwnerIdAndOwnerType(disbursement.getMilestoneId(), "MILESTONE").orElseThrow();
        // Minus talent amount + mentor amount + system fee from milestone wallet
        BigDecimal totalOut = disbursement.getMentorAmount().add(disbursement.getTalentAmount()).add(disbursement.getSystemFee());
        if (systemWallet.getBalance().compareTo(totalOut) < 0) {
            throw new BusinessException("Số dư ví hệ thống không đủ để thực hiện giải ngân");
        }
        systemWallet.setBalance(systemWallet.getBalance().subtract(totalOut));

        // Add system fee to system wallet
        Wallet systemFeeWallet = getOrCreateSystemWallet();
        systemFeeWallet.setBalance(systemFeeWallet.getBalance().add(disbursement.getSystemFee()));

        createTransaction(systemFeeWallet, disbursement.getSystemFee(), "CREDIT", "DISBURSEMENT_IN",
                "Nhận phí hệ thống từ giải ngân Milestone", disbursement.getId(), "DISBURSEMENT",
                disbursement.getProjectId(), disbursement.getMilestoneId());

        // Add mentor amount to team mentor wallet
        Wallet teamMentorWallet = getOrCreateTeamWallet(disbursement.getMilestoneId(), "TEAM_MENTOR");
        teamMentorWallet.setBalance(teamMentorWallet.getBalance().add(disbursement.getMentorAmount()));

        createTransaction(teamMentorWallet, disbursement.getMentorAmount(), "CREDIT", "DISBURSEMENT_IN",
                "Nhận tiền giải ngân Milestone", disbursement.getId(), "DISBURSEMENT",
                disbursement.getProjectId(), disbursement.getMilestoneId());

        // Add talent amount to team talent wallet
        Wallet teamTalentWallet = getOrCreateTeamWallet(disbursement.getMilestoneId(), "TEAM_TALENT");
        teamTalentWallet.setBalance(teamTalentWallet.getBalance().add(disbursement.getTalentAmount()));

        createTransaction(teamTalentWallet, disbursement.getTalentAmount(), "CREDIT", "DISBURSEMENT_IN",
                "Nhận tiền giải ngân Milestone", disbursement.getId(), "DISBURSEMENT",
                disbursement.getProjectId(), disbursement.getMilestoneId());

        disbursement.setStatus(Status.COMPLETED.toString());
        disbursementRepository.save(disbursement);
        walletRepository.save(systemWallet);
        walletRepository.save(teamMentorWallet);
        walletRepository.save(teamTalentWallet);

        LeaderInfo mentor = null;
        LeaderInfo talent = null;

        for (LeaderInfo leader : leaderInfoResponse.getLeadersList()) {
            if (Role.MENTOR.toString().equals(leader.getRoleInProject())) {
                mentor = leader;
            } else if (Role.TALENT.toString().equals(leader.getRoleInProject())) {
                talent = leader;
            }
        }

        // Nếu không tìm thấy mentor hoặc talent thì throw exception
        if (mentor == null) {
            throw new BusinessException("Không tìm thấy leader MENTOR cho milestone");
        }
        if (talent == null) {
            throw new BusinessException("Không tìm thấy leader TALENT cho milestone");
        }

        // Tạo đối tượng LeaderDisbursementInfo cho MENTOR
        LeaderDisbursementInfo mentorObj = LeaderDisbursementInfo.builder()
                .userId(mentor.getUserId())
                .fullName(mentor.getFullName())
                .email(mentor.getEmail())
                .avatarUrl(mentor.getAvatarUrl())
                .isLeader(true)
                .roleInProject(Role.MENTOR.toString())
                .amount(disbursement.getMentorAmount())
                .build();

        // Tạo đối tượng LeaderDisbursementInfo cho TALENT
        LeaderDisbursementInfo talentObj = LeaderDisbursementInfo.builder()
                .userId(talent.getUserId())
                .fullName(talent.getFullName())
                .email(talent.getEmail())
                .avatarUrl(talent.getAvatarUrl())
                .isLeader(true)
                .roleInProject(Role.TALENT.toString())
                .amount(disbursement.getTalentAmount())
                .build();

        return DisbursementCalculationResponse.builder()
                .milestoneId(disbursement.getMilestoneId().toString())
                .totalAmount(disbursement.getTotalAmount())
                .systemFee(disbursement.getSystemFee())
                .mentorLeader(mentorObj)
                .talentLeader(talentObj)
                .status(disbursement.getStatus())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<Void> processMilestoneDisbursement(UUID milestoneId, UUID userId, MilestoneDisbursementRequest request) {
        // 1. Validate Input
        if (request.getDisbursements() == null || request.getDisbursements().isEmpty()) {
            throw new BusinessException("Danh sách phân bổ không được trống");
        }

        UUID sourceWalletId = request.getWalletId();
        List<MilestoneDisbursement> payload = request.getDisbursements();

        // 2. Fetch Source Wallet (Milestone Team Wallet)
        Wallet sourceWallet = walletRepository.findById(sourceWalletId)
                .orElseThrow(() -> new BusinessException("Ví nguồn không tồn tại"));

        // Optional: Validate that the source wallet belongs to the correct Milestone and Owner Type
        // Assuming wallet.ownerId stored the milestoneId for TEAM wallets
        if (!sourceWallet.getOwnerId().equals(milestoneId)) {
            throw new BusinessException("Ví này không thuộc về cột mốc hiện tại");
        }

        Disbursement disbursement = disbursementRepository.findByMilestoneId(milestoneId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin giải ngân cho cột mốc này."));

        String ownerType = sourceWallet.getOwnerType(); // TEAM_MENTOR or TEAM_TALENT
        if ("TEAM_MENTOR".equals(ownerType)) {
            if (!userId.equals(disbursement.getMentorLeaderId())) {
                throw new BusinessException("Bạn không phải là trưởng nhóm.");
            }
        } else if ("TEAM_TALENT".equals(ownerType)) {
            if (!userId.equals(disbursement.getTalentLeaderId())) {
                throw new BusinessException("Bạn không phải là trưởng nhóm.");
            }
        } else {
            throw new BusinessException("Loại ví không hợp lệ để thực hiện phân bổ");
        }

        // 4. Calculate Total Out
        BigDecimal totalOut = payload.stream()
                .map(MilestoneDisbursement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Check Balance
        if (sourceWallet.getBalance().compareTo(totalOut) < 0) {
            throw new BusinessException("Số dư quỹ nhóm không đủ để thực hiện phân bổ (Số dư: "
                    + sourceWallet.getBalance() + ", Tổng chi: " + totalOut + ")");
        }

        // 6. Deduct from Source Wallet
        sourceWallet.setBalance(sourceWallet.getBalance().subtract(totalOut));
        walletRepository.save(sourceWallet);

        // Record Debit Transaction for Source Wallet
        createTransaction(
                sourceWallet,
                totalOut,
                "DISBURSEMENT_DISTRIBUTION",
                PaymentConstant.DEBIT,
                "Phân bổ tiền cho thành viên",
                userId,
                "MILESTONE",
                null,
                milestoneId
        );

        // 7. Process Each Recipient
        for (MilestoneDisbursement item : payload) {
            UUID memberId = item.getUserId();
            BigDecimal amount = item.getAmount();

            if (amount.compareTo(BigDecimal.ZERO) <= 0) continue;

            // Fetch or Create Member Wallet
            Wallet memberWallet = walletRepository.findByOwnerId(memberId)
                    .orElseThrow(() -> new BusinessException("Thành viên với ID " + memberId + " không có ví trong hệ thống"));

            // Add to Member Wallet
            memberWallet.setBalance(memberWallet.getBalance().add(amount));
            walletRepository.save(memberWallet);

            // Record Credit Transaction for Member
            createTransaction(
                    memberWallet,
                    amount,
                    "DISBURSEMENT_RECEIVED",
                    PaymentConstant.CREDIT,
                    "Nhận tiền phân bổ từ cột mốc",
                    milestoneId,
                    "MILESTONE",
                    null,
                    milestoneId
            );
        }

        return ApiResponse.success("Phân bổ tiền thành công", null);
    }

    @Override
    public ApiResponse<DisbursementResponse> getByMilestoneId(UUID milestoneId) {

        Disbursement disbursement = disbursementRepository
                .findByMilestoneId(milestoneId)
                .orElseThrow(() ->
                        new BusinessException("Chưa có thông tin giải ngân cho cột mốc.")
                );

        DisbursementResponse response = DisbursementResponse.builder()
                .disbursementId(disbursement.getId())
                .milestoneId(disbursement.getMilestoneId())
                .projectId(disbursement.getProjectId())
                .totalAmount(disbursement.getTotalAmount())
                .systemFee(disbursement.getSystemFee())
                .mentorAmount(disbursement.getMentorAmount())
                .mentorLeaderId(disbursement.getMentorLeaderId())
                .talentAmount(disbursement.getTalentAmount())
                .talentLeaderId(disbursement.getTalentLeaderId())
                .status(disbursement.getStatus())
                .updatedAt(disbursement.getUpdatedAt())
                .build();

        return ApiResponse.success(response);
    }

    private Wallet getOrCreateTeamWallet(UUID ownerId, String ownerType) {
        return walletRepository.findByOwnerIdAndOwnerType(ownerId, ownerType)
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet();
                    newWallet.setOwnerId(ownerId);
                    newWallet.setOwnerType(ownerType);
                    newWallet.setBalance(BigDecimal.ZERO);
                    newWallet.setHeldBalance(BigDecimal.ZERO);
                    newWallet.setCurrency("VND");
                    newWallet.setStatus(Status.ACTIVE.toString());
                    newWallet.setBankInfos(new ArrayList<>());
                    return walletRepository.save(newWallet);
                });
    }

    private Wallet getOrCreateWallet(UUID userId) {
        return walletRepository.findByOwnerId(userId)
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setOwnerId(userId);
                    w.setOwnerType(Role.COMPANY.toString()); // Mặc định là COMPANY nếu là người thanh toán
                    w.setBalance(BigDecimal.ZERO);
                    w.setHeldBalance(BigDecimal.ZERO);
                    w.setCurrency("VND");
                    w.setStatus(Status.ACTIVE.toString());
                    return walletRepository.save(w);
                });
    }

    private Wallet getOrCreateSystemWallet() {
        return walletRepository.findByOwnerType("SYSTEM")
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setOwnerType("SYSTEM");
                    w.setOwnerId(UUID.randomUUID()); // ID ảo
                    w.setBalance(BigDecimal.ZERO);
                    w.setHeldBalance(BigDecimal.ZERO);
                    w.setCurrency("VND");
                    w.setStatus(Status.ACTIVE.toString());
                    return walletRepository.save(w);
                });
    }

    private void createTransaction(Wallet wallet, BigDecimal amount, String type, String direction,
                                   String desc, UUID refId, String refType,
                                   UUID projectId, UUID milestoneId) {
        Transaction tx = Transaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(type)
                .direction(direction)
                .description(desc)
                .refId(refId)
                .refType(refType)
                .projectId(projectId)
                .milestoneId(milestoneId)
                .status(Status.SUCCESS.toString())
                .balanceAfter(wallet.getBalance())
                .build();
        transactionRepository.save(tx);
    }
}
